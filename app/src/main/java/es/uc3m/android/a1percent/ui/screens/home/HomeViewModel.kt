package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.CreditManager
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.XpManager
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var goalsJob: Job? = null

    init {
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user) }
                    startObservingData(user.id)
                } else {
                    stopObservingData()
                    _uiState.value = HomeUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingData(userId: String) {
        tasksJob?.cancel()
        goalsJob?.cancel()

        tasksJob = TaskRespository.observeTasks(userId)
            .onEach { tasks ->
                _uiState.update { current ->
                    val profileMap = buildSharedProfileMap(tasks, userId)
                    reduceHomeState(current.copy(tasks = tasks, sharedUserProfilesById = profileMap, currentUserId = userId))
                }
            }
            .launchIn(viewModelScope)

        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                _uiState.update { it.copy(
                    goals = goals,
                    goal = goals.firstOrNull()
                ) }
                checkWeeklyRituals(goals)
            }
            .launchIn(viewModelScope)

        SocialRepository.observeFriends(userId)
            .onEach { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
            .launchIn(viewModelScope)

        UserRepository.allUsers
            .onEach { _ ->
                _uiState.update { current ->
                    current.copy(sharedUserProfilesById = buildSharedProfileMap(current.tasks, userId))
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            CreditManager.resetCreditsIfNeeded(userId)
        }
    }

    private fun buildSharedProfileMap(tasks: List<Task>, userId: String): Map<String, UserProfile> {
        val sharedIds = tasks.flatMap { it.sharedWith }.toSet() - userId
        return sharedIds.associateWith { UserRepository.findUserById(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }
    }

    private fun stopObservingData() {
        tasksJob?.cancel()
        goalsJob?.cancel()
    }

    private fun checkWeeklyRituals(goals: List<Goal>) {
        val now = System.currentTimeMillis()

        val pendingGoal = goals.firstOrNull { goal ->
            goal.status == GoalStatus.ACTIVE
                && goal.aiRoadmapStatus == AiRoadmapStatus.READY
                && goal.nextGenerationDate != null
                && now >= goal.nextGenerationDate
        } ?: return

        _uiState.update { it.copy(navigateToRitual = pendingGoal.id) }
    }

    fun onMissionsFilterToggled() {
        _uiState.update { current ->
            val next = when (current.filters.missionsFilter) {
                HomeMissionsFilter.ALL -> HomeMissionsFilter.MISSIONS
                HomeMissionsFilter.MISSIONS -> HomeMissionsFilter.TASKS
                HomeMissionsFilter.TASKS -> HomeMissionsFilter.ALL
            }
            val updatedFilters = current.filters.copy(missionsFilter = next)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }

    fun onSortByDateToggled() {
        _uiState.update { current ->
            val nextSort = if (current.filters.sortBy == HomeSort.DATE_ASC) {
                HomeSort.NONE
            } else {
                HomeSort.DATE_ASC
            }
            val updatedFilters = current.filters.copy(sortBy = nextSort)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }

    fun onStatusFilterToggled() {
        _uiState.update { current ->
            val nextStatus = when (current.filters.statusFilter) {
                HomeStatusFilter.PENDING -> HomeStatusFilter.COMPLETED
                HomeStatusFilter.COMPLETED -> HomeStatusFilter.ALL
                HomeStatusFilter.ALL -> HomeStatusFilter.PENDING
            }
            val updatedFilters = current.filters.copy(statusFilter = nextStatus)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }

    fun onFilterClicked(filterKey: HomeFilterKey) {
        when (filterKey) {
            HomeFilterKey.STATUS -> onStatusFilterToggled()
            HomeFilterKey.MISSIONS -> onMissionsFilterToggled()
            HomeFilterKey.SORT_BY_DATE -> onSortByDateToggled()
        }
    }

    fun onTaskChecked(taskId: String) {
        val userId = SessionRepository.currentUser.value?.id ?: return
        val task = _uiState.value.tasks.find { it.id == taskId } ?: return
        val wasCompleted = userId in task.completedBy
        viewModelScope.launch {
            TaskRespository.toggleTaskCompletion(taskId, userId)
            if (wasCompleted) {
                XpManager.revokeTaskXp(userId, task)
            } else {
                val now = System.currentTimeMillis()
                XpManager.awardTaskXp(userId, task.copy(completedAt = now))
            }
            task.goalId?.let { goalId -> recalculateGoalProgress(goalId, userId) }
        }
    }

    private suspend fun recalculateGoalProgress(goalId: String, userId: String) {
        val allTasks = _uiState.value.tasks
        val goalTasks = allTasks.filter { it.goalId == goalId && it.isAiGenerated }
        if (goalTasks.isEmpty()) return
        val latestWeek = goalTasks.maxOf { it.weekNumber ?: 0 }
        val currentWeekTasks = goalTasks.filter { it.weekNumber == latestWeek }
        if (currentWeekTasks.isEmpty()) return
        val completed = currentWeekTasks.count { userId in it.completedBy }
        val progress = (completed * 100) / currentWeekTasks.size
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        if (goal.progress != progress) {
            GoalRepository.updateGoal(goal.copy(progress = progress))
        }
    }

    fun onTaskClicked(task: Task) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    fun dismissTaskDetail() {
        _uiState.update { it.copy(selectedTask = null) }
    }

    fun onTaskPostpone(taskId: String) {
        _uiState.update { it.copy(showDatePickerForTask = taskId) }
    }

    fun onDatePickerResult(taskId: String, epochDay: Long) {
        viewModelScope.launch {
            TaskRespository.updateTaskDeadline(taskId, TaskDeadline.OnDate(epochDay))
        }
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onDatePickerDismissed() {
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onShareTaskRequested(task: Task) {
        _uiState.update { it.copy(showShareSheet = true, shareTargetTask = task) }
    }

    fun onShareWithFriend(friendUserId: String, friendName: String) {
        val task = _uiState.value.shareTargetTask ?: return
        viewModelScope.launch {
            TaskRespository.shareTask(task.id, friendUserId).onSuccess {
                _uiState.update { it.copy(
                    showShareSheet = false,
                    shareTargetTask = null,
                    snackbarMessage = "Shared with $friendName!"
                ) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    showShareSheet = false,
                    shareTargetTask = null,
                    snackbarMessage = "Error: ${error.message}"
                ) }
            }
        }
    }

    fun onShareDismissed() {
        _uiState.update { it.copy(showShareSheet = false, shareTargetTask = null) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onRitualNavigated() {
        _uiState.update { it.copy(navigateToRitual = null) }
    }

    private fun applyFiltersAndSort(tasks: List<Task>, filters: HomeFilters): List<Task> {
        val currentUserId = SessionRepository.currentUser.value?.id ?: ""
        val statusFiltered = when (filters.statusFilter) {
            HomeStatusFilter.PENDING -> tasks.filter { currentUserId !in it.completedBy }
            HomeStatusFilter.COMPLETED -> tasks.filter { currentUserId in it.completedBy }
            HomeStatusFilter.ALL -> tasks
        }

        val filtered = when (filters.missionsFilter) {
            HomeMissionsFilter.ALL -> statusFiltered
            HomeMissionsFilter.MISSIONS -> statusFiltered.filter { it.goalId != null }
            HomeMissionsFilter.TASKS -> statusFiltered.filter { it.goalId == null }
        }

        return when (filters.sortBy) {
            HomeSort.NONE -> filtered.sortedWith(TaskDeadlineResolver.taskDeadlineComparator())
            HomeSort.DATE_ASC -> filtered.sortedBy { TaskDeadlineResolver.toSortKey(it.deadline) }
        }
    }

    private fun reduceHomeState(base: HomeUiState): HomeUiState {
        val visibleTasks = applyFiltersAndSort(base.tasks, base.filters)
        val filterItems = buildHomeFilterUiItems(base.filters)
        val syncedSelectedTask = base.selectedTask?.let { selected ->
            base.tasks.find { it.id == selected.id }
        }
        return base.copy(
            visibleTasks = visibleTasks,
            filterItems = filterItems,
            selectedTask = syncedSelectedTask
        )
    }

    fun onProfileClicked(): String {
        return SessionRepository.currentUser.value?.name ?: "Unknown User"
    }
}
