package es.uc3m.android.a1percent.ui.screens.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.WeeklySummaryRepository
import es.uc3m.android.a1percent.data.XpManager
import es.uc3m.android.a1percent.data.ai.AICoachService
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.isDeadlineWeek
import es.uc3m.android.a1percent.data.model.weeksRemaining
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class RitualViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RitualUiState())
    val uiState: StateFlow<RitualUiState> = _uiState.asStateFlow()

    companion object {
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    fun loadRitual(goalId: String) {
        val userId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            val goal = GoalRepository.getGoalById(goalId).getOrNull() ?: return@launch
            val tasks = TaskRespository.getTasks(userId).getOrNull() ?: emptyList()
            val goalTasks = tasks.filter { it.goalId == goalId && it.isAiGenerated }

            val completed = goalTasks.count { it.status == TaskStatus.COMPLETED }
            val epicTask = goalTasks.maxByOrNull { it.dayIndex ?: 0 }
            val epicPassed = epicTask?.status == TaskStatus.COMPLETED
            val xpEarned = goalTasks.filter { it.status == TaskStatus.COMPLETED }
                .sumOf { it.xpAwarded ?: it.xp }

            val now = System.currentTimeMillis()
            val isCatchUp = (now - (goal.nextGenerationDate ?: 0)) > SEVEN_DAYS_MILLIS * 2

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()
            val weekNumber = (latestSummary?.weekNumber ?: 0) + 1

            val steps = computeVisibleSteps(goal, isCatchUp)

            _uiState.update {
                RitualUiState(
                    goal = goal,
                    visibleSteps = steps,
                    tasksCompleted = completed,
                    totalTasks = goalTasks.size,
                    epicMissionPassed = epicPassed,
                    xpEarned = xpEarned,
                    oldIntensity = goal.currentIntensity,
                    isCatchUp = isCatchUp,
                    weekNumber = weekNumber,
                    goalProgress = goal.progress,
                    weeksRemaining = goal.weeksRemaining()
                )
            }
        }
    }

    private fun computeVisibleSteps(goal: Goal, isCatchUp: Boolean): List<RitualStep> {
        if (isCatchUp) {
            return listOf(
                RitualStep.SUMMARY,
                RitualStep.FEEDBACK,
                RitualStep.INTENSITY_CHANGE,
                RitualStep.GENERATING,
                RitualStep.COMPLETE
            )
        }

        val steps = mutableListOf(
            RitualStep.SUMMARY,
            RitualStep.EPIC_RESULT
        )

        val now = System.currentTimeMillis()
        if (goal.deadline in now..(now + SEVEN_DAYS_MILLIS)) {
            steps.add(RitualStep.DEADLINE_CHECK)
        }

        steps.add(RitualStep.FEEDBACK)
        steps.add(RitualStep.INTENSITY_CHANGE)
        steps.add(RitualStep.GENERATING)
        steps.add(RitualStep.COMPLETE)
        return steps
    }

    fun onNextStep() {
        _uiState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
    }

    fun onSkipToFeedback() {
        val feedbackIndex = _uiState.value.visibleSteps.indexOf(RitualStep.FEEDBACK)
        if (feedbackIndex >= 0) {
            _uiState.update { it.copy(currentStepIndex = feedbackIndex) }
        }
    }

    fun onFeedbackSelected(feedback: EnergyFeedback) {
        val goal = _uiState.value.goal ?: return
        val isCatchUp = _uiState.value.isCatchUp

        val newIntensity = if (isCatchUp) {
            AICoachService.calculateCatchUpIntensity(goal, feedback.name)
        } else {
            AICoachService.calculateNewIntensity(
                goal = goal,
                epicPassed = _uiState.value.epicMissionPassed,
                feedback = feedback.name
            )
        }

        _uiState.update {
            it.copy(
                selectedFeedback = feedback,
                newIntensity = newIntensity,
                currentStepIndex = it.currentStepIndex + 1
            )
        }
    }

    fun onExtendDeadline(newDeadline: Long) {
        _uiState.update {
            it.copy(
                newDeadline = newDeadline,
                showDatePicker = false
            )
        }
        onNextStep()
    }

    fun onCompleteGoal() {
        val goal = _uiState.value.goal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return

        viewModelScope.launch {
            val completedGoal = goal.copy(
                status = GoalStatus.COMPLETED,
                progress = 100
            )
            GoalRepository.updateGoal(completedGoal)
            XpManager.awardGoalCompletionBonus(userId, goal)
            _uiState.update { it.copy(goalCompleted = true) }
        }
    }

    fun onShowDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun onStartGeneration() {
        val goal = _uiState.value.goal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return
        val feedback = _uiState.value.selectedFeedback ?: return
        val newIntensity = _uiState.value.newIntensity ?: return
        val isCatchUp = _uiState.value.isCatchUp
        val weekNumber = _uiState.value.weekNumber

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()

            if (!isCatchUp) {
                val summary = WeeklySummary(
                    goalId = goal.id,
                    userId = userId,
                    weekNumber = weekNumber - 1,
                    tasksCompleted = _uiState.value.tasksCompleted,
                    totalTasks = _uiState.value.totalTasks,
                    epicMissionPassed = _uiState.value.epicMissionPassed,
                    userFeedback = feedback,
                    intensityUsed = goal.currentIntensity
                )
                WeeklySummaryRepository.saveSummary(goal.id, summary)

                if (_uiState.value.epicMissionPassed) {
                    XpManager.awardEpicWeeklyBonus(userId, goal)
                }

                val totalWeeks = ((goal.deadline - goal.createdAt) / SEVEN_DAYS_MILLIS).toInt().coerceAtLeast(1)
                val newProgress = ((weekNumber * 100) / totalWeeks).coerceIn(0, 100)
                GoalRepository.updateGoal(goal.copy(progress = newProgress))
            }

            var updatedGoal = goal.copy(currentIntensity = newIntensity)

            if (_uiState.value.newDeadline != null) {
                updatedGoal = updatedGoal.copy(
                    deadline = _uiState.value.newDeadline!!,
                    extensionCount = goal.extensionCount + 1
                )
            }

            val isWeekend = Calendar.getInstance().let {
                it.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            }

            val nextWeekIsDeadline = run {
                val nextWeekStart = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                val nextWeekEnd = nextWeekStart + SEVEN_DAYS_MILLIS
                updatedGoal.deadline in nextWeekStart..nextWeekEnd
            }

            val result = AICoachService.generateWeeklyTasks(
                goal = updatedGoal,
                weeklySummary = if (isCatchUp) null else latestSummary,
                isWeekend = isWeekend,
                userFeedback = feedback.name,
                userId = userId,
                weekNumber = weekNumber,
                isDeadlineWeek = nextWeekIsDeadline
            )

            result.onSuccess { tasks ->
                TaskRespository.saveTaskBatch(userId, tasks.map { it.copy(goalId = goal.id) })

                val finalGoal = updatedGoal.copy(
                    nextGenerationDate = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                )
                GoalRepository.updateGoal(finalGoal)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generationComplete = true,
                        currentStepIndex = it.currentStepIndex + 1
                    )
                }
            }

            result.onFailure {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }
}
