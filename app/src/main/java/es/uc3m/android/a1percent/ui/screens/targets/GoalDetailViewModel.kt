package es.uc3m.android.a1percent.ui.screens.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.UserRepository
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

data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList(),
    val selectedMission: Task? = null,
    val showDatePickerForTask: String? = null,
    val editingTask: Task? = null,
    val showShareSheet: Boolean = false,
    val friends: List<UserProfile> = emptyList(),
    val sharedWithProfiles: List<UserProfile> = emptyList(),
    val snackbarMessage: String? = null
)

class GoalDetailViewModel : ViewModel() {
    private var currentGoalId: String? = null
    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private var goalsJob: Job? = null
    private var tasksJob: Job? = null
    private var socialJob: Job? = null
    private var profilesJob: Job? = null

    init {
        SessionRepository.currentUser
            .onEach { user ->
                val goalId = currentGoalId
                if (user == null || goalId == null) {
                    stopObserving()
                    _uiState.value = GoalDetailUiState()
                } else {
                    currentUserId = user.id
                    startObserving(user.id, goalId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadGoal(goalId: String) {
        currentGoalId = goalId
        val userId = SessionRepository.currentUser.value?.id ?: return
        currentUserId = userId
        startObserving(userId, goalId)
    }

    private fun startObserving(userId: String, goalId: String) {
        stopObserving()

        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                val goal = goals.find { it.id == goalId }
                val sharedProfiles = resolveSharedProfiles(goal, userId)
                _uiState.update { it.copy(goal = goal, sharedWithProfiles = sharedProfiles) }
            }
            .launchIn(viewModelScope)

        tasksJob = TaskRespository.observeTasks(userId)
            .onEach { tasks ->
                _uiState.update { it.copy(missions = tasks.filter { t -> t.goalId == goalId }) }
            }
            .launchIn(viewModelScope)

        socialJob = SocialRepository.observeFriends(userId)
            .onEach { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
            .launchIn(viewModelScope)

        profilesJob = UserRepository.allUsers
            .onEach { _ ->
                val goal = _uiState.value.goal
                _uiState.update { it.copy(sharedWithProfiles = resolveSharedProfiles(goal, userId)) }
            }
            .launchIn(viewModelScope)
    }

    private fun resolveSharedProfiles(goal: Goal?, userId: String): List<UserProfile> {
        return goal?.sharedWith
            ?.filter { it != userId }
            ?.mapNotNull { UserRepository.findUserById(it) }
            ?: emptyList()
    }

    private fun stopObserving() {
        goalsJob?.cancel()
        tasksJob?.cancel()
        socialJob?.cancel()
        profilesJob?.cancel()
    }

    fun onMissionClicked(task: Task) {
        _uiState.update { it.copy(selectedMission = task) }
    }

    fun onCloseMissionDetail() {
        _uiState.update { it.copy(selectedMission = null) }
    }

    fun onTaskComplete(taskId: String) {
        viewModelScope.launch {
            TaskRespository.updateTaskStatus(taskId, TaskStatus.COMPLETED).onSuccess {
                applyLocalTaskStatus(taskId, TaskStatus.COMPLETED)
            }
        }
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

    fun onTaskEdit(task: Task) {
        _uiState.update { it.copy(editingTask = task) }
    }

    fun onTaskUpdate(task: Task) {
        viewModelScope.launch {
            TaskRespository.updateTask(task)
        }
        _uiState.update { current ->
            current.copy(
                editingTask = null,
                missions = current.missions.map { if (it.id == task.id) task else it }
            )
        }
    }

    fun onEditDismissed() {
        _uiState.update { it.copy(editingTask = null) }
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
                    snackbarMessage = "Shared with $friendName!"
                ) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    showShareSheet = false,
                    snackbarMessage = "Error: ${error.message}"
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

    private fun applyLocalTaskStatus(taskId: String, status: TaskStatus) {
        _uiState.update { current ->
            current.copy(
                missions = current.missions.map { mission ->
                    if (mission.id == taskId) mission.copy(status = status) else mission
                }
            )
        }
    }
}
