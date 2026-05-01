package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TargetsViewModel : ViewModel() {

    private var allGoals: List<Goal> = emptyList()
    private var allTasks: List<Task> = emptyList()

    private val _uiState = MutableStateFlow(TargetsUiState())
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var goalsJob: Job? = null

    init {
        SessionRepository.currentUser
            .onEach { user ->
                if (user == null) {
                    stopObservingData()
                    allGoals = emptyList()
                    allTasks = emptyList()
                    _uiState.value = TargetsUiState()
                } else {
                    startObservingData(user.id)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingData(userId: String) {
        tasksJob?.cancel()
        goalsJob?.cancel()

        tasksJob = TaskRespository.observeTasks(userId)
            .onEach { tasks ->
                allTasks = tasks
                _uiState.update { current -> reduceTargetsState(current) }
            }
            .launchIn(viewModelScope)

        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                allGoals = goals
                _uiState.update { current -> reduceTargetsState(current) }
            }
            .launchIn(viewModelScope)

        SocialRepository.observeFriends(userId)
            .onEach { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
            .launchIn(viewModelScope)
    }

    private fun stopObservingData() {
        tasksJob?.cancel()
        goalsJob?.cancel()
    }

    private fun applyTaskFiltersAndSort(filters: TaskFilters): List<Task> {
        val statusFiltered = filters.selectedStatus?.let { status ->
            allTasks.filter { it.status == status }
        } ?: allTasks

        val filtered = if (filters.quickFilters.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter { task ->
                filters.quickFilters.all { filter ->
                    when (filter) {
                        TaskQuickFilter.MISSIONS -> task.goalId != null
                        TaskQuickFilter.SHARED -> true  // TODO: replace with real shared/collaboration source
                    }
                }
            }
        }

        return when (filters.sort) {
            TaskSort.NONE -> filtered
            TaskSort.DEADLINE_ASC -> filtered.sortedBy { task ->
                TaskDeadlineResolver.toSortKey(task.deadline)
            }
            TaskSort.XP_DESC -> filtered.sortedByDescending { it.xp }
        }
    }

    private fun applyGoalFiltersAndSort(filters: GoalFilters): List<Goal> {
        return when (filters.sort) {
            GoalSort.NONE -> allGoals
            GoalSort.PROGRESS_DESC -> allGoals.sortedByDescending { it.progress }
        }
    }


    // TAB VIEW
    fun onTabSelected(tab: TargetsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // FILTERING
    fun onTaskFilterClicked(filterKey: TaskFilterKey) {
        _uiState.update { current ->
            val updatedTaskFilters = when (filterKey) {
                TaskFilterKey.MISSIONS -> toggleQuickFilter(current.taskFilters, TaskQuickFilter.MISSIONS)
                TaskFilterKey.SHARED -> toggleQuickFilter(current.taskFilters, TaskQuickFilter.SHARED)
                TaskFilterKey.CATEGORY -> current.taskFilters
                TaskFilterKey.SORT -> current.taskFilters.copy(sort = nextTaskSort(current.taskFilters.sort))
            }
            reduceTargetsState(current.copy(taskFilters = updatedTaskFilters))
        }
    }

    fun onTaskStatusFilterClicked() {
        _uiState.update { current ->
            val updatedTaskFilters = current.taskFilters.copy(
                selectedStatus = nextTaskStatusFilter(current.taskFilters.selectedStatus)
            )
            reduceTargetsState(current.copy(taskFilters = updatedTaskFilters))
        }
    }

    fun onGoalFilterClicked(filterKey: GoalFilterKey) {
        if (filterKey != GoalFilterKey.SORT) return
        _uiState.update { current ->
            val updatedGoalFilters = current.goalFilters.copy(sort = nextGoalSort(current.goalFilters.sort))
            reduceTargetsState(current.copy(goalFilters = updatedGoalFilters))
        }
    }

    @Suppress("unused")
    fun onTaskCategoryClick() {
        // TODO: open category selector and apply advanced category filter.
    }

    fun onTaskClicked(task: Task) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    fun onCloseTaskDetail() {
        _uiState.update { it.copy(selectedTask = null) }
    }

    // GOAL DETAIL VIEW

    @Suppress("UNUSED_PARAMETER")
    fun onGoalClicked(goalId: String) {
        // TODO: navigate to goal detail when detail flow is implemented.
    }


    // ACTIONS

    fun onTaskComplete(taskId: String) {
        viewModelScope.launch {
            TaskRespository.updateTaskStatus(taskId, TaskStatus.COMPLETED).onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = "Error completing task: ${error.message ?: "unknown error"}")
                }
            }
        }
    }

    fun onTaskPostpone(taskId: String) {
        _uiState.update { it.copy(showDatePickerForTask = taskId) }
    }

    fun onDatePickerResult(taskId: String, epochDay: Long) {
        viewModelScope.launch {
            TaskRespository.updateTaskDeadline(taskId, TaskDeadline.OnDate(epochDay)).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating deadline: ${error.message}") }
            }
        }
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onDatePickerDismissed() {
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onTaskEdit(task: Task) {
        _uiState.update { it.copy(editingTask = task) }
    }

    fun onTaskUpdate(task: Task) {
        viewModelScope.launch {
            TaskRespository.updateTask(task).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating task: ${error.message}") }
            }
        }
        _uiState.update { it.copy(editingTask = null) }
    }

    fun onEditDismissed() {
        _uiState.update { it.copy(editingTask = null) }
    }

    fun onGoalDeleteRequested(goalId: String) {
        _uiState.update { it.copy(showDeleteGoalConfirm = goalId) }
    }

    fun onGoalDeleteConfirmed() {
        val goalId = _uiState.value.showDeleteGoalConfirm ?: return
        viewModelScope.launch {
            GoalRepository.deleteGoalWithMissions(goalId).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error deleting goal: ${error.message}") }
            }
        }
        _uiState.update { it.copy(showDeleteGoalConfirm = null) }
    }

    fun onGoalDeleteDismissed() {
        _uiState.update { it.copy(showDeleteGoalConfirm = null) }
    }

    fun onShareTaskRequested(task: Task) {
        _uiState.update { it.copy(showShareSheet = true, shareTargetTask = task, shareTargetGoal = null) }
    }

    fun onShareGoalRequested(goal: Goal) {
        _uiState.update { it.copy(showShareSheet = true, shareTargetGoal = goal, shareTargetTask = null) }
    }

    fun onShareWithFriend(friendUserId: String, friendName: String) {
        viewModelScope.launch {
            val task = _uiState.value.shareTargetTask
            val goal = _uiState.value.shareTargetGoal
            val result = if (task != null) {
                TaskRespository.shareTask(task.id, friendUserId)
            } else if (goal != null) {
                GoalRepository.shareGoal(goal.id, friendUserId)
            } else return@launch

            result.onSuccess {
                _uiState.update { it.copy(
                    showShareSheet = false,
                    shareTargetTask = null,
                    shareTargetGoal = null,
                    snackbarMessage = "Shared with $friendName"
                ) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error sharing: ${error.message}") }
            }
        }
    }

    fun onShareDismissed() {
        _uiState.update { it.copy(showShareSheet = false, shareTargetTask = null, shareTargetGoal = null) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onTaskDelete(taskId: String) {
        viewModelScope.launch {
            TaskRespository.deleteTask(taskId).onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = "Error deleting task: ${error.message ?: "unknown error"}")
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun reduceTargetsState(base: TargetsUiState): TargetsUiState {
        return base.copy(
            tasks = applyTaskFiltersAndSort(base.taskFilters),
            goals = applyGoalFiltersAndSort(base.goalFilters),
            goalTitleById = allGoals.associate { it.id to it.title },
            taskFilterItems = buildTaskFilterUiItems(base.taskFilters),
            goalFilterItems = buildGoalFilterUiItems(base.goalFilters)
        )
    }

    private fun nextTaskStatusFilter(current: TaskStatus?): TaskStatus? {
        return when (current) {
            null -> TaskStatus.PENDING
            TaskStatus.PENDING -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> null
        }
    }

    private fun toggleQuickFilter(filters: TaskFilters, filter: TaskQuickFilter): TaskFilters {
        val updated = if (filters.quickFilters.contains(filter)) {
            filters.quickFilters - filter
        } else {
            filters.quickFilters + filter
        }
        return filters.copy(quickFilters = updated)
    }

    private fun nextTaskSort(current: TaskSort): TaskSort {
        return when (current) {
            TaskSort.NONE -> TaskSort.DEADLINE_ASC
            TaskSort.DEADLINE_ASC -> TaskSort.XP_DESC
            TaskSort.XP_DESC -> TaskSort.NONE
        }
    }

    private fun nextGoalSort(current: GoalSort): GoalSort {
        return when (current) {
            GoalSort.NONE -> GoalSort.PROGRESS_DESC
            GoalSort.PROGRESS_DESC -> GoalSort.NONE
        }
    }
}


