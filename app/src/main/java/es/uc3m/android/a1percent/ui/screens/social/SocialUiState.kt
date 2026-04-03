package es.uc3m.android.a1percent.ui.screens.social

import es.uc3m.android.a1percent.data.model.UserProfile

data class SocialUiState(
    val currentUser: UserProfile? = null,
    val friends: List<UserProfile> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

