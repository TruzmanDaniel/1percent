package es.uc3m.android.a1percent.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.MockData
import kotlinx.coroutines.flow.*

/**
 * ViewModel to manage Profile screen state and logic.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            user = UserRepository.currentUser.value ?: MockData.mockUser
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Observe changes in the current user from the Repository
        UserRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { currentState ->
                        currentState.copy(user = user)
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

