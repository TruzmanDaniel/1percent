package es.uc3m.android.a1percent.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.UserRelationship
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userObserverJob: Job? = null

    fun loadUser(userId: String?) {
        userObserverJob?.cancel()

        val sessionUser = SessionRepository.currentUser.value ?: run {
            _uiState.update { it.copy(user = null) }
            return
        }

        if (userId == null || userId == "placeholder" || userId == sessionUser.id) {
            // Observe the session user StateFlow so changes (e.g. vacation mode toggle)
            // are immediately reflected in the Profile UI.
            userObserverJob = viewModelScope.launch {
                SessionRepository.currentUser.collect { current ->
                    _uiState.update {
                        it.copy(
                            user = current,
                            isOwnProfile = true,
                            relationshipStatus = null,
                            relationship = null,
                            currentUserId = current?.id ?: sessionUser.id
                        )
                    }
                }
            }
        } else {
            userObserverJob = UserRepository.allUsers
                .onEach { users ->
                    val targetUser = users.find { it.id == userId }
                    val relationship = getRelationship(sessionUser.id, userId)
                    if (targetUser != null) {
                        _uiState.update {
                            it.copy(
                                user = targetUser,
                                isOwnProfile = false,
                                relationshipStatus = relationship?.status,
                                relationship = relationship,
                                currentUserId = sessionUser.id
                            )
                        }
                    } else {
                        _uiState.update { it.copy(user = null, isOwnProfile = false, relationshipStatus = null, relationship = null, currentUserId = sessionUser.id) }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun getRelationship(currentUserId: String, targetUserId: String): UserRelationship? {
        return SocialRepository.getRelationshipBetween(currentUserId, targetUserId)
    }

    fun onFriendAction(targetUserId: String) {
        val currentUserId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.sendFriendRequest(currentUserId, targetUserId)
            _uiState.update {
                it.copy(
                    relationshipStatus = RelationshipStatus.PENDING,
                    relationship = UserRelationship(currentUserId, targetUserId, RelationshipStatus.PENDING)
                )
            }
        }
    }

    fun onAcceptRequest(targetUserId: String) {
        val currentUserId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.acceptFriendRequest(targetUserId, currentUserId)
            _uiState.update {
                it.copy(
                    relationshipStatus = RelationshipStatus.FRIENDS,
                    relationship = UserRelationship(targetUserId, currentUserId, RelationshipStatus.FRIENDS)
                )
            }
        }
    }

    fun onRejectRequest(targetUserId: String) {
        val currentUserId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.rejectFriendRequest(targetUserId, currentUserId)
            _uiState.update {
                it.copy(
                    relationshipStatus = null,
                    relationship = null
                )
            }
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

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingUsername = true, usernameError = null, profileUpdateSuccess = null) }
            val result = SessionRepository.updateUsername(newUsername)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingUsername = false,
                            user = state.user?.copy(name = newUsername),
                            profileUpdateSuccess = "Username updated successfully"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUpdatingUsername = false, usernameError = e.message) }
                }
            )
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPassword = true, passwordError = null, profileUpdateSuccess = null) }
            val result = SessionRepository.updatePassword(currentPassword, newPassword)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUpdatingPassword = false, profileUpdateSuccess = "Password updated successfully") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUpdatingPassword = false, passwordError = e.message) }
                }
            )
        }
    }

    fun clearProfileUpdateSuccess() {
        _uiState.update { it.copy(profileUpdateSuccess = null) }
    }

    fun onToggleVacationMode() {
        val user = uiState.value.user ?: return
        val currentUser = SessionRepository.currentUser.value ?: return
        if (user.id != currentUser.id) return

        viewModelScope.launch {
            if (currentUser.isVacationMode) {
                GoalRepository.deactivateVacationMode(currentUser.id)
                val updated = currentUser.copy(isVacationMode = false, vacationStartDate = null)
                SessionRepository.updateUserProfile(updated)
            } else {
                GoalRepository.activateVacationMode(currentUser.id)
                val updated = currentUser.copy(
                    isVacationMode = true,
                    vacationStartDate = System.currentTimeMillis()
                )
                SessionRepository.updateUserProfile(updated)
            }
        }
    }
}
