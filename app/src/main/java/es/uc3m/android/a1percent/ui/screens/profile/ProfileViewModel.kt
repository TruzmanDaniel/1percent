package es.uc3m.android.a1percent.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUser(userId: String?) {
        val sessionUser = SessionRepository.currentUser.value ?: run {
            _uiState.update { it.copy(user = null) }
            return
        }

        if (userId == null || userId == "placeholder" || userId == sessionUser.id) {
            _uiState.update { it.copy(user = sessionUser, isOwnProfile = true, relationshipStatus = null) }
        } else {
            val targetUser = UserRepository.findUserById(userId)
            val relStatus = getRelationshipStatus(sessionUser.id, userId)
            if (targetUser != null) {
                _uiState.update { it.copy(user = targetUser, isOwnProfile = false, relationshipStatus = relStatus) }
            } else {
                _uiState.update { it.copy(user = null, isOwnProfile = false) }
            }
        }
    }

    private fun getRelationshipStatus(currentUserId: String, targetUserId: String): RelationshipStatus? {
        val table = SocialRepository.friendshipTable.value
        val rel = table.find { r ->
            (r.userAId == currentUserId && r.userBId == targetUserId) ||
            (r.userAId == targetUserId && r.userBId == currentUserId)
        }
        return rel?.status
    }

    fun onFriendAction(targetUserId: String) {
        val currentUserId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.sendFriendRequest(currentUserId, targetUserId)
            _uiState.update { it.copy(relationshipStatus = RelationshipStatus.PENDING) }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, uploadError = null) }
            val result = SessionRepository.uploadProfilePicture(uri)
            result.fold(
                onSuccess = { url ->
                    _uiState.update { state ->
                        state.copy(
                            isUploadingAvatar = false,
                            user = state.user?.copy(avatarUrl = url)
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUploadingAvatar = false, uploadError = e.message) }
                }
            )
        }
    }
}
