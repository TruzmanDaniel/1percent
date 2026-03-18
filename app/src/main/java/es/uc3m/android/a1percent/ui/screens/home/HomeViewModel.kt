package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.MockData
import kotlinx.coroutines.flow.*

/**
 * Data class to manage logic and screen state. It lets the interface observe changes and update
 */
class HomeViewModel : ViewModel() {

    // The UI State is now derived from the SessionRepository's current user
    private val _uiState = MutableStateFlow(
        HomeUiState(
            user = SessionRepository.currentUser.value ?: MockData.mockUser, // session user
            tasks = MockData.mockTasks,
            goal = MockData.mockGoal
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe changes in the current user from the SessionRepository
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { currentState ->
                        currentState.copy(user = user)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // Function that returns a parameter to be passed (it is an example)
    fun onProfileClicked(): String {
        return SessionRepository.currentUser.value?.name ?: "Unknown User"
    }
}
