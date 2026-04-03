package es.uc3m.android.a1percent.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class SocialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        // Load initial data
        val currentUser = SessionRepository.currentUser.value
        _uiState.update { it.copy(currentUser = currentUser) }

        val currentId = currentUser?.id
        if (currentId != null) {
            SocialRepository.observeFriends(currentId)
                .onEach { friends ->
                    _uiState.update { it.copy(friends = friends) }
                }
                .launchIn(viewModelScope)
        }
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
