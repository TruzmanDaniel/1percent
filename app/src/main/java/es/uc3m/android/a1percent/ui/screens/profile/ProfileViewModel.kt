package es.uc3m.android.a1percent.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.UserRepository
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
            _uiState.update { it.copy(user = sessionUser, isOwnProfile = true) }
        } else {
            val targetUser = UserRepository.findUserById(userId)
            if (targetUser != null) {
                _uiState.update { it.copy(user = targetUser, isOwnProfile = false) }
            } else {
                _uiState.update { it.copy(user = null, isOwnProfile = false) }
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
}
