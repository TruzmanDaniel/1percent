package es.uc3m.android.a1percent.ui.screens.profile

import es.uc3m.android.a1percent.data.model.UserProfile

/**
 * Data class to represent the Profile screen state (DATA to be displayed by the UI).
 */
data class ProfileUiState(
    val user: UserProfile,
    val isOwnProfile: Boolean = true
)
