package es.uc3m.android.a1percent.ui.screens.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.ai.AICoachService
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateGoalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGoalUiState())
    val uiState: StateFlow<CreateGoalUiState> = _uiState.asStateFlow()

    private var currentIntensity: Float = 3f

    fun onGoalNameChange(newValue: String) {
        _uiState.update { it.copy(goalName = newValue) }
    }

    fun onDifficultyChange(newValue: Float) {
        _uiState.update { it.copy(difficulty = newValue) }
        currentIntensity = newValue
    }

    fun onDeadlineSelected(epochMillis: Long) {
        _uiState.update { it.copy(deadlineEpochMillis = epochMillis, showDatePicker = false) }
    }

    fun onShowDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun generateProposal() {
        val userId = SessionRepository.currentUser.value?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiNegotiationState.GENERATING, errorMessage = null) }

            val isWeekend = Calendar.getInstance().let {
                it.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            }

            val tempGoal = Goal(
                title = _uiState.value.goalName,
                category = Category.PERSONAL,
                difficulty = _uiState.value.difficulty.toInt(),
                xp = _uiState.value.difficulty.toInt() * 50,
                currentIntensity = currentIntensity,
                deadline = _uiState.value.deadlineEpochMillis
                    ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
            )

            val result = AICoachService.generateWeeklyTasks(
                goal = tempGoal,
                weeklySummary = null,
                isWeekend = isWeekend,
                userFeedback = null,
                weekNumber = 1
            )

            result.onSuccess { aiResult ->
                _uiState.update {
                    it.copy(
                        aiState = AiNegotiationState.PROPOSAL_READY,
                        proposedTasks = aiResult.tasks,
                        availableCredits = aiResult.availableCredits
                    )
                }
            }

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        aiState = AiNegotiationState.ERROR,
                        errorMessage = error.message ?: "Error generating tasks"
                    )
                }
            }
        }
    }

    fun adjustAndRegenerate(isEasier: Boolean) {
        if (!_uiState.value.canNegotiate) return

        currentIntensity = if (isEasier) {
            currentIntensity * 1.20f
        } else {
            currentIntensity * 0.80f
        }

        _uiState.update { it.copy(negotiationCount = it.negotiationCount + 1) }
        generateProposal()
    }

    fun acceptProposal(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = SessionRepository.currentUser.value?.id
        if (userId == null) {
            onError("No user logged in")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val now = System.currentTimeMillis()
                val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000

                val goal = Goal(
                    title = _uiState.value.goalName,
                    category = Category.PERSONAL,
                    difficulty = _uiState.value.difficulty.toInt(),
                    xp = _uiState.value.difficulty.toInt() * 50,
                    currentIntensity = currentIntensity,
                    deadline = _uiState.value.deadlineEpochMillis
                        ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                    nextGenerationDate = now + sevenDaysMillis,
                    aiRoadmapStatus = AiRoadmapStatus.READY
                )

                val goalResult = GoalRepository.saveGoal(userId, goal)
                goalResult.onFailure { throw it }

                val tasksWithGoalId = _uiState.value.proposedTasks.map {
                    it.copy(goalId = goal.id)
                }

                val taskResult = TaskRespository.saveTaskBatch(userId, tasksWithGoalId)
                taskResult.onFailure { throw it }

                _uiState.update { it.copy(aiState = AiNegotiationState.ACCEPTED) }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error saving goal")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createGoal(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = SessionRepository.currentUser.value?.id
        if (userId == null) {
            onError("No user logged in")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val goal = Goal(
                    title = _uiState.value.goalName,
                    category = Category.PERSONAL,
                    difficulty = _uiState.value.difficulty.toInt(),
                    xp = _uiState.value.difficulty.toInt() * 50,
                    deadline = _uiState.value.deadlineEpochMillis
                        ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                )

                val result = GoalRepository.saveGoal(userId, goal)

                result.onSuccess { onSuccess() }
                result.onFailure { onError(it.message ?: "Error creating goal") }
            } catch (e: Exception) {
                onError(e.message ?: "Error creating goal")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateGoalUiState()
        currentIntensity = 3f
    }
}
