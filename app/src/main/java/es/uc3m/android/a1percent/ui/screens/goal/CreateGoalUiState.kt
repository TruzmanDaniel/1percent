package es.uc3m.android.a1percent.ui.screens.goal

import es.uc3m.android.a1percent.data.model.Task

data class CreateGoalUiState(
    val goalName: String = "",
    val difficulty: Float = 3f,
    val hasDeadline: Boolean = false,
    val deadlineEpochMillis: Long? = null,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val aiState: AiNegotiationState = AiNegotiationState.IDLE,
    val proposedTasks: List<Task> = emptyList(),
    val negotiationCount: Int = 0,
    val errorMessage: String? = null,
    val availableCredits: Int = 5
) {
    val canCreateGoal: Boolean
        get() = goalName.isNotBlank() && !isLoading && aiState != AiNegotiationState.GENERATING

    val canNegotiate: Boolean
        get() = negotiationCount < 3 && aiState == AiNegotiationState.PROPOSAL_READY

    val canGenerateAi: Boolean
        get() = goalName.isNotBlank() && !isLoading && availableCredits > 0
            && aiState != AiNegotiationState.GENERATING
}
