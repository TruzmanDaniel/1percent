package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Targets browsing.
 * Step 1: mock lists + basic tab/filter state.
 */
class TargetsViewModel : ViewModel() {

    private var allGoals: List<Goal> = emptyList()
    private var allTasks: List<Task> = emptyList()

    private val _uiState = MutableStateFlow(TargetsUiState())
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()

    init {
        SessionRepository.currentUser
            .onEach { user ->
                if (user == null) {
                    allGoals = emptyList()
                    allTasks = emptyList()
                    _uiState.value = TargetsUiState()
                } else {
                    loadTargetsData(user.id)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTargetsData(userId: String) {
        viewModelScope.launch {
            TaskRespository.getTasks(userId).onSuccess { tasks ->
                allTasks = tasks
            }
            GoalRepository.getGoals(userId).onSuccess { goals ->
                allGoals = goals
            }

            _uiState.update { current ->
                reduceTargetsState(current)
            }
        }
    }

    private fun applyTaskFiltersAndSort(filters: TaskFilters): List<Task> {
        val filtered = if (filters.quickFilters.isEmpty()) {
            allTasks
        } else {
            allTasks.filter { task ->
                filters.quickFilters.all { filter ->
                    when (filter) {
                        TaskQuickFilter.MISSIONS -> task.goalId != null
                        TaskQuickFilter.SHARED -> true // TODO: replace with real shared/collaboration source
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

    fun onTaskCategoryClick() {
        // TODO: open category selector and apply advanced category filter.
    }

    fun onGoalFilterClicked(filterKey: GoalFilterKey) {
        if (filterKey != GoalFilterKey.SORT) return
        _uiState.update { current ->
            val updatedGoalFilters = current.goalFilters.copy(sort = nextGoalSort(current.goalFilters.sort))
            reduceTargetsState(current.copy(goalFilters = updatedGoalFilters))
        }
    }

    // DETAIL VIEW

    @Suppress("UNUSED_PARAMETER")
    fun onGoalClicked(goalId: String) {
        // TODO: navigate to goal detail when detail flow is implemented.
    }

    fun onTaskClicked(task: Task) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    fun onCloseTaskDetail() {
        _uiState.update { it.copy(selectedTask = null) }
    }

    // ACTIONS

    // TODO: Implement actual task action logic (currently just logs)
    fun onTaskComplete(taskId: String) {
        // TODO: Update task status to COMPLETED in repository
        println("Task $taskId marked as complete")
    }

    fun onTaskPostpone(taskId: String) {
        // TODO: Update task deadline (move to next period) in repository
        println("Task $taskId postponed")
    }

    fun onTaskDelete(taskId: String) {
        // TODO: Persist deletion in repository and refresh
        allTasks = allTasks.filter { it.id != taskId }
        _uiState.update { current ->
            reduceTargetsState(
                current.copy(selectedTask = current.selectedTask?.takeIf { it.id != taskId })
            )
        }
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
