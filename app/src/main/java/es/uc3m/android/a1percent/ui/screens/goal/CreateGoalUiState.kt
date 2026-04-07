package es.uc3m.android.a1percent.ui.screens.goal

/**
 * UI state for the goal creation flow.
 * Minimal implementation for now, ready for future expansion.
 */
data class CreateGoalUiState(
    val goalName: String = "",
    val difficulty: Float = 3f
) {
    // Ready for future properties when goal definition is expanded
    // e.g., category, deadline, description, etc.

    val canCreateGoal: Boolean
        get() = goalName.isNotBlank()
}
