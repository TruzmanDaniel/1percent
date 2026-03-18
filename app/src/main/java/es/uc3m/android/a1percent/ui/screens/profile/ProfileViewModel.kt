package es.uc3m.android.a1percent.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.*

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(user = SessionRepository.currentUser.value ?: MockData.mockUser)
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Loads the profile data based on the provided ID.
     * If userId is null or matches current session, it shows the session user.
     */
    fun loadUser(userId: String?) {
        val sessionUser = SessionRepository.currentUser.value ?: return
        
        if (userId == null || userId == "placeholder" || userId == sessionUser.id) {
            // It's my own profile
            _uiState.update { it.copy(user = sessionUser, isOwnProfile = true) }
        } else {
            // It's someone else's profile
            val targetUser = UserRepository.findUserById(userId)
            if (targetUser != null) {
                _uiState.update { it.copy(user = targetUser, isOwnProfile = false) }
            }
        }
    }
}
