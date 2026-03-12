package es.uc3m.android.a1percent.ui.screens.home

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile

/**
 * Data class to represent the screen state (DATA to be displayed by the UI)
 */

data class HomeUiState(
    val user: UserProfile,
    val tasks: List<Task>,
    val goal: Goal
)
