package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
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
                current.copy(
                    tasks = applyTaskFilters(current.selectedTaskFilters),
                    goals = allGoals,
                    goalTitleById = allGoals.associate { it.id to it.title }
                )
            }
        }
    }

    private fun applyTaskFilters(filters: Set<TaskQuickFilter>): List<Task> {
        if (filters.isEmpty()) return allTasks

        return allTasks.filter { task ->
            filters.all { filter ->
                when (filter) {
                    TaskQuickFilter.MISSIONS -> task.goalId != null
                    TaskQuickFilter.SHARED -> true // TODO: replace with real shared/collaboration source
                }
            }
        }
    }

    // TAB VIEW
    fun onTabSelected(tab: TargetsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // FILTERING
    fun onTaskFilterSelected(filter: TaskQuickFilter) {
        _uiState.update { current ->
            // Toggle: if filter is already selected, remove it; otherwise, add it
            val newSelectedFilters = if (current.selectedTaskFilters.contains(filter)) {
                current.selectedTaskFilters - filter
            } else {
                current.selectedTaskFilters + filter
            }

            current.copy(
                selectedTaskFilters = newSelectedFilters,
                tasks = applyTaskFilters(newSelectedFilters)
            )
        }
    }

    fun onTaskCategoryClick() {
        // TODO: open category selector and apply advanced category filter.
    }

    fun onTaskSortClick() {
        // TODO: open sorting options (deadline, creation date, difficulty, etc.).
    }

    fun onGoalSortClick() {
        // TODO: open sorting options for goals (progress, deadline, createdAt, etc.).
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
            current.copy(
                tasks = applyTaskFilters(current.selectedTaskFilters),
                selectedTask = current.selectedTask?.takeIf { it.id != taskId }
            )
        }
    }
}
