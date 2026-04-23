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
    private var currentGoalId: String? = null

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        SessionRepository.currentUser
            .onEach { user ->
                val goalId = currentGoalId
                if (user == null || goalId == null) {
                    _uiState.value = GoalDetailUiState()
                } else {
                    loadGoalForUser(user.id, goalId)
                }
            }
            .launchIn(viewModelScope)
    }

    // Load goal screen by the parameter goalId in the route/url
    fun loadGoal(goalId: String) {
        currentGoalId = goalId
        val userId = SessionRepository.currentUser.value?.id ?: return
        loadGoalForUser(userId, goalId)
    }

    private fun loadGoalForUser(userId: String, goalId: String) {
        viewModelScope.launch {
            var goal: Goal? = null
            var missions: List<Task> = emptyList()

            GoalRepository.getGoals(userId).onSuccess { goals ->
                goal = goals.find { it.id == goalId }
            }
            TaskRespository.getTasks(userId).onSuccess { tasks ->
                missions = tasks.filter { it.goalId == goalId }
            }

            _uiState.update {
                it.copy(goal = goal, missions = missions)
            }
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

