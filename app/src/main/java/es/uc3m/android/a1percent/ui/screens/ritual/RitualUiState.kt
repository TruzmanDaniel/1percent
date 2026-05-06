package es.uc3m.android.a1percent.ui.screens.ritual

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback

data class RitualUiState(
    val goal: Goal? = null,
    val visibleSteps: List<RitualStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0,
    val epicMissionPassed: Boolean = false,
    val xpEarned: Int = 0,
    val selectedFeedback: EnergyFeedback? = null,
    val newIntensity: Float? = null,
    val oldIntensity: Float? = null,
    val milestoneReached: Int? = null,
    val newDeadline: Long? = null,
    val isGenerating: Boolean = false,
    val generationComplete: Boolean = false,
    val isCatchUp: Boolean = false,
    val goalCompleted: Boolean = false,
    val showDatePicker: Boolean = false,
    val weekNumber: Int = 0,
    val newWeeklyStreak: Int = 0
) {
    val currentStep: RitualStep?
        get() = visibleSteps.getOrNull(currentStepIndex)

    val canSkip: Boolean
        get() = currentStep in listOf(RitualStep.SUMMARY, RitualStep.EPIC_RESULT, RitualStep.INTENSITY_CHANGE)

    val isLastStep: Boolean
        get() = currentStepIndex >= visibleSteps.lastIndex
}
