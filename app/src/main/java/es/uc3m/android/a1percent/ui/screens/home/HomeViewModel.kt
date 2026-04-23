package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.TaskRespository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

/**
 * Data class to manage logic and screen state. It lets the interface observe changes and update
 */
class HomeViewModel : ViewModel() {

    // The UI state is remote-backed: session user + Firestore tasks/goals.
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe changes in the current user from the SessionRepository
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user) }
                    loadUserData(user.id)
                } else {
                    _uiState.value = HomeUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            // Load tasks
            TaskRespository.getTasks(userId).onSuccess { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
            // Load first goal
            GoalRepository.getGoals(userId).onSuccess { goals ->
                if (goals.isNotEmpty()) {
                    _uiState.update { it.copy(goal = goals.first()) }
                }
            }
        }
    }

    // Function that returns a parameter to be passed (it is an example)
    fun onProfileClicked(): String {
        return SessionRepository.currentUser.value?.name ?: "Unknown User"
    }
}
