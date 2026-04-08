package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for Targets browsing.
 * Step 1: mock lists + basic tab/filter state.
 */
class TargetsViewModel : ViewModel() {

     private val allGoals: List<Goal> = listOf(

         // Mock data  TODO remove mock data
        MockData.mockGoal,
        Goal(
            id = "goal-2",
            title = "Build Consistent Fitness Routine",
            description = "Train 4 days per week and track recovery.",
            category = Category.FITNESS,
            difficulty = 3,
            xp = 180,
            deadline = null,
            status = GoalStatus.ACTIVE,
            progress = 55,
            createdAt = 1741737600000
        ),
        Goal(
            id = "goal-3",
            title = "Improve Personal Finance Basics",
            description = "Create monthly budget and save emergency funds.",
            category = Category.FINANCE,
            difficulty = 2,
            xp = 140,
            deadline = null,
            status = GoalStatus.PAUSED,
            progress = 20,
            createdAt = 1741737600000
        )
    )
    private val allTasks: List<Task> = MockData.mockTasks



    private val _uiState = MutableStateFlow(
        TargetsUiState(
            tasks = allTasks,
            // tasks = allTasks {it.goal == null},
            goals = allGoals,
            goalTitleById = allGoals.associate { it.id to it.title },
            selectedTaskFilters = emptySet()
        )
    )
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: TargetsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onTaskFilterSelected(filter: TaskQuickFilter) {
        _uiState.update { current ->
            // Toggle: if filter is already selected, remove it; otherwise, add it
            val newSelectedFilters = if (current.selectedTaskFilters.contains(filter)) {
                current.selectedTaskFilters - filter
            } else {
                current.selectedTaskFilters + filter
            }

            // Filter tasks based on selected filters
            // If no filters selected, show all tasks
            val filteredTasks = if (newSelectedFilters.isEmpty()) {
                allTasks
            } else {
                allTasks.filter { task ->
                    newSelectedFilters.all { filter ->
                        when (filter) {
                            TaskQuickFilter.TASKS -> task.goalId == null
                            TaskQuickFilter.MISSIONS -> task.goalId != null
                            TaskQuickFilter.SHARED -> true // TODO: replace with real collaboration/shared query
                        }
                    }
                }
            }

            current.copy(
                selectedTaskFilters = newSelectedFilters,
                tasks = filteredTasks
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
        // TODO: Delete task from repository
        _uiState.update { current ->
            current.copy(tasks = current.tasks.filter { it.id != taskId })
        }
    }
}
