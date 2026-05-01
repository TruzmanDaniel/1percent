package es.uc3m.android.a1percent.ui.screens.profile

import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus

data class ProfileUiState(
    val user: UserProfile? = null,
    val isOwnProfile: Boolean = true,
    val isUploadingAvatar: Boolean = false,
    val uploadError: String? = null,
    val relationshipStatus: RelationshipStatus? = null
)
