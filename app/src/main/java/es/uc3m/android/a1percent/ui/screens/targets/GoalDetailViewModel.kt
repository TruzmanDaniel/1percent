package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList(),
    val selectedMission: Task? = null,
    val showShareSheet: Boolean = false,
    val friends: List<UserProfile> = emptyList(),
    val snackbarMessage: String? = null
)

/**
 * ViewModel for Goal Detail screen.
 * Handles goal lookup and mission management (complete, delete actions).
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
                    SocialRepository.observeFriends(user.id)
                        .onEach { friends ->
                            _uiState.update { it.copy(friends = friends) }
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }

    // Load goal screen by the parameter goalId in the route/url
    fun loadGoal(goalId: String) {
        currentGoalId = goalId
        val userId = SessionRepository.currentUser.value?.id ?: return
        loadGoalForUser(userId, goalId)
        SocialRepository.observeFriends(userId)
            .onEach { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
            .launchIn(viewModelScope)
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

    fun onMissionClicked(task: Task) {
        _uiState.update { it.copy(selectedMission = task) }
    }

    fun onCloseMissionDetail() {
        _uiState.update { it.copy(selectedMission = null) }
    }

    fun onTaskComplete(taskId: String) {
        val userId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            TaskRespository.toggleTaskCompletion(taskId, userId).onSuccess {
                applyLocalToggleCompletion(taskId, userId)
            }
        }
    }

    fun onTaskDelete(taskId: String) {
        viewModelScope.launch {
            TaskRespository.deleteTask(taskId)
        }
        _uiState.update { current ->
            current.copy(missions = current.missions.filter { it.id != taskId })
        }
    }

    fun onShareGoalRequested() {
        _uiState.update { it.copy(showShareSheet = true) }
    }

    fun onShareWithFriend(friendUserId: String, friendName: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            GoalRepository.shareGoal(goal.id, friendUserId).onSuccess {
                _uiState.update { it.copy(
                    showShareSheet = false,
                    snackbarMessage = "Shared with $friendName"
                ) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    showShareSheet = false,
                    snackbarMessage = "Error sharing: ${error.message}"
                ) }
            }
        }
    }

    fun onShareDismissed() {
        _uiState.update { it.copy(showShareSheet = false) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun applyLocalToggleCompletion(taskId: String, userId: String) {
        _uiState.update { current ->
            current.copy(
                missions = current.missions.map { mission ->
                    if (mission.id == taskId) {
                        val updated = if (userId in mission.completedBy) {
                            mission.completedBy - userId
                        } else {
                            mission.completedBy + userId
                        }
                        mission.copy(completedBy = updated)
                    } else mission
                }
            )
        }
    }
}

