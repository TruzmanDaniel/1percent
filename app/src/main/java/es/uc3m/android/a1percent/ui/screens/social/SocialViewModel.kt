package es.uc3m.android.a1percent.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class SocialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()
    private var friendsObserverJob: Job? = null // job is a background task (coroutine),doesn't block app cycle


    init {
        observeSession()
    }


    // Observes session changes and sync 'friends' list accordingly.
    // Reacts to login/logout/user changes by updating UI state and managing friends
    private fun observeSession() {
        // Observamos el StateFlow completo y no .value puntual --> Reactivo a login/logout.
        SessionRepository.currentUser
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user, friends = emptyList()) } // Update uistate with new user and cleans friends

                friendsObserverJob?.cancel() // cancelled when user changes (avoids mixing friends lists)
                if (user != null) {
                    friendsObserverJob = SocialRepository.observeFriends(user.id)    // the job we launch
                        .onEach { friends ->
                            _uiState.update { it.copy(friends = friends) }     // .onEach: each time someone is added, or removed to/from the list, a new emission of the list is received
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope) // esto activa el Flow declarado antes; lo ejecuta mientras el ViewModel está vivo -> ligado al ciclo de vida
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        if (newQuery.length >= 2) {
            val results = UserRepository.searchUsers(newQuery)
                .filter { it.id != SessionRepository.currentUser.value?.id } // Don't show myself
            _uiState.update { it.copy(searchResults = results) }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun sendFriendRequest(toUserId: String) {
        val currentId = SessionRepository.currentUser.value?.id ?: return
        SocialRepository.sendFriendRequest(currentId, toUserId)
    }
}
