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
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var friendsObserverJob: Job? = null
    private var requestsObserverJob: Job? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        SessionRepository.currentUser
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user, friends = emptyList(), pendingRequests = emptyList()) }

                friendsObserverJob?.cancel()
                requestsObserverJob?.cancel()

                if (user != null) {
                    friendsObserverJob = SocialRepository.observeFriends(user.id)
                        .onEach { friends ->
                            _uiState.update { it.copy(friends = friends) }
                        }
                        .launchIn(viewModelScope)

                    requestsObserverJob = SocialRepository.observePendingRequests(user.id)
                        .onEach { requests ->
                            _uiState.update { it.copy(pendingRequests = requests) }
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        if (newQuery.length >= 2) {
            val results = UserRepository.searchUsers(newQuery)
                .filter { it.id != SessionRepository.currentUser.value?.id }
            _uiState.update { it.copy(searchResults = results) }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun sendFriendRequest(toUserId: String) {
        val currentId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.sendFriendRequest(currentId, toUserId)
        }
    }

    fun acceptRequest(requesterId: String) {
        val currentId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.acceptFriendRequest(requesterId, currentId)
        }
    }

    fun rejectRequest(requesterId: String) {
        val currentId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            SocialRepository.rejectFriendRequest(requesterId, currentId)
        }
    }
}
