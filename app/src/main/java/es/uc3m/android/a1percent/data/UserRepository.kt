package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository to manage user session and data.
 * This is the bridge before implementing Firebase.
 */
object UserRepository {
    // Shared state of the current user across the app
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    /**
     * Attempts to login by searching in MockData.
     * Returns true if successful, false otherwise.
     */
    fun login(email: String, pass: String): Boolean {
        val user = MockData.allMockUsers.find { it.email == email && it.password == pass }
        return if (user != null) {
            _currentUser.value = user
            true
        } else {
            false
        }
    }

    /**
     * Clears the current session.
     */
    fun logout() {
        _currentUser.value = null
    }
}
