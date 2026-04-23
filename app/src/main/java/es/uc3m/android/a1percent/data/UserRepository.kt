package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository to manage user database and social relationships.
 * Responsible for finding other users, public profiles, and community interactions.
 */
object UserRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Local cache backed by Firestore snapshot updates
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    init {
        observeUsersCollection()
    }

    private fun observeUsersCollection() {
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val users = snapshot.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val username = doc.getString("username")
                        ?: doc.getString("name")
                        ?: email.substringBefore("@")

                    UserProfile(
                        id = doc.id,
                        name = username,
                        email = email,
                        level = doc.getLong("level")?.toInt() ?: 1,
                        currentXp = doc.getLong("currentXp")?.toInt() ?: 0,
                        xpToNextLevel = doc.getLong("xpToNextLevel")?.toInt() ?: 100, // TODO es correcto dar valores default a esto?
                        avatarUrl = doc.getString("avatarUrl"),
                        streakDays = doc.getLong("streakDays")?.toInt() ?: 0,
                        totalTasksCompleted = doc.getLong("totalTasksCompleted")?.toInt() ?: 0
                    )
                }

                _allUsers.update { users }
            }
    }

    /**
     * Finds a public profile by its unique ID.
     */
    fun findUserById(userId: String): UserProfile? {
        return _allUsers.value.find { it.id == userId }
    }

    /**
     * Search for users by name (for social feature).
     */
    fun searchUsers(query: String): List<UserProfile> {
        return _allUsers.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Get suggestions based on user interests or random.
     */
    fun getSuggestedUsers(): List<UserProfile> {
        return _allUsers.value.take(5)
    }
}
