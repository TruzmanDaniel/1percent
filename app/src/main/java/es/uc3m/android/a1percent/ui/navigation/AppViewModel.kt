package es.uc3m.android.a1percent.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import es.uc3m.android.a1percent.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Root ViewModel for the app — lives for the entire app session.
 * This is the right place for things that must run regardless of which screen is visible,
 * like detecting incoming friend requests and firing notifications.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    // Tracks requester IDs already seen so we only notify for genuinely new ones.
    // Null = first emission (initial load) — record silently, do not notify.
    private var knownRequestIds: Set<String>? = null
    private var requestsObserverJob: Job? = null

    private var knownFriendIds: Set<String>? = null
    private var acceptedObserverJob: Job? = null

    init {
        viewModelScope.launch {
            SessionRepository.restoreCurrentUserIfAvailable()
        }
        observeFriendRequests()
        observeAcceptedRequests()
    }

    private fun observeFriendRequests() {
        SessionRepository.currentUser
            .onEach { user ->
                requestsObserverJob?.cancel()
                knownRequestIds = null // reset when the logged-in user changes

                if (user != null) {
                    requestsObserverJob = SocialRepository.observePendingRequests(user.id)
                        .onEach { requests ->
                            val currentIds = requests.map { it.id }.toSet()

                            if (knownRequestIds == null) {
                                // First emission = requests that existed before the app opened.
                                knownRequestIds = currentIds
                            } else {
                                // Any ID not seen before is a new incoming request.
                                val newRequests = requests.filter { it.id !in knownRequestIds!! }
                                newRequests.forEach { requester ->
                                    NotificationHelper.notifyFriendRequest(
                                        context = getApplication(),
                                        fromName = requester.name
                                    )
                                }
                                knownRequestIds = currentIds
                            }
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAcceptedRequests() {
        SessionRepository.currentUser
            .onEach { user ->
                acceptedObserverJob?.cancel()
                knownFriendIds = null // reset when the logged-in user changes

                if (user != null) {
                    acceptedObserverJob = SocialRepository.friendshipTable
                        .onEach { relations ->
                            val acceptedIds = relations
                                .filter { it.userAId == user.id && it.status == RelationshipStatus.FRIENDS }
                                .map { it.userBId }
                                .toSet()

                            if (knownFriendIds == null) {
                                knownFriendIds = acceptedIds
                            } else {
                                val newlyAccepted = acceptedIds - knownFriendIds!!
                                newlyAccepted.forEach { friendId ->
                                    val name = UserRepository.findUserById(friendId)?.name ?: return@forEach
                                    NotificationHelper.notifyFriendAccepted(
                                        context = getApplication(),
                                        byName = name
                                    )
                                }
                                knownFriendIds = acceptedIds
                            }
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }
}
