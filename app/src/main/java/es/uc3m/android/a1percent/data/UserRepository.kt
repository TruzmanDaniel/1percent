package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository to manage user database and social relationships.
 * Responsible for finding other users, public profiles, and community interactions.
 */
object UserRepository {
    // List of all known users (Mock database)
    private val allUsers = MockData.allMockUsers

    /**
     * Finds a public profile by its unique ID.
     */
    fun findUserById(userId: String): UserProfile? {
        return allUsers.find { it.id == userId }
    }

    /**
     * Search for users by name (for social feature).
     */
    fun searchUsers(query: String): List<UserProfile> {
        return allUsers.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Get suggestions based on user interests or random.
     */
    fun getSuggestedUsers(): List<UserProfile> {
        return allUsers.take(5)
    }
}
