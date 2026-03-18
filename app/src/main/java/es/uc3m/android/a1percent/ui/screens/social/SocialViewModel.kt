package es.uc3m.android.a1percent.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.*

data class SocialUiState(
    val currentUser: UserProfile? = null,
    val friends: List<UserProfile> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class SocialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        // Load initial data
        val current = SessionRepository.currentUser.value
        _uiState.update { it.copy(currentUser = current) }
        
        loadFriends()
        
        // Listen to changes in the friendship table
        SocialRepository.friendshipTable
            .onEach { loadFriends() }
            .launchIn(viewModelScope)
    }

    private fun loadFriends() {
        val currentId = SessionRepository.currentUser.value?.id ?: return
        val friendsList = SocialRepository.getFriendsOfUser(currentId)
        _uiState.update { it.copy(friends = friendsList) }
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
