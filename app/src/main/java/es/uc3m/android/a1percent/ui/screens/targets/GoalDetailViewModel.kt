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

data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList()
)

/**
 * ViewModel for Goal Detail screen.
 * Handles goal lookup and mission management (complete, postpone, delete actions).
 * TODO: Replace mock data with real repository once backend is integrated.
 */
class GoalDetailViewModel : ViewModel() {

    // Mock goals and tasks (same as in TargetsViewModel)
    private val allGoals: List<Goal> = listOf(
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

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    fun loadGoal(goalId: String) {
        val goal = allGoals.find { it.id == goalId }
        val missions = allTasks.filter { it.goalId == goalId }

        _uiState.update {
            it.copy(goal = goal, missions = missions)
        }
    }

    // TODO: Implement actual task update logic (currently just logs action)
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
            current.copy(missions = current.missions.filter { it.id != taskId })
        }
    }
}

