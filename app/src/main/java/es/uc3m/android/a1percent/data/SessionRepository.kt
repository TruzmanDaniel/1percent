package es.uc3m.android.a1percent.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectSerializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Singleton repository to manage the active user session.
 * Responsible for AUTHENTICATION and IDENTIFICATION.
 */
object SessionRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Shared state of the CURRENT authenticated user
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    suspend fun restoreCurrentUserIfAvailable() {
        val firebaseUser = auth.currentUser ?: return

        // 1. Fallback user so the app can keep working immediately after restart
        _currentUser.value = UserProfile(
            id = firebaseUser.uid,
            name = firebaseUser.email?.substringBefore("@") ?: firebaseUser.uid,
            email = firebaseUser.email ?: ""
        )

        // 2. Modern sequential Firestore call with await()
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val profile = doc.toObjectSerializable<UserProfile>()
            if (profile != null) {
                _currentUser.value = profile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun registerWithFirebaseAndApi(
        email: String,
        password: String,
        username: String
    ): Result<Unit> {
        return try {
            // 1. Create User on Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario en FirebaseAuth")

            // 2. Prepare UserProfile object
            val user = UserProfile(
                id = firebaseUser.uid,
                name = username,
                email = email,
                password = "",
                createdAt = System.currentTimeMillis(),
                level = 1,
                currentXp = 0,
                xpToNextLevel = 100,
                avatarUrl = null,
                streakDays = 0,
                totalTasksCompleted = 0
            )

            // 3. Create the user document using id (serialize data class)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.uid)
                .set(user.encodeToMap()!!)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithFirebase(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            // Protect the sign-in operation with a timeout to avoid hanging the UI
            withTimeout(10_000L) {
                auth.signInWithEmailAndPassword(email, password).await()
            }

            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                restoreCurrentUserIfAvailable()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears the current session.
     */
    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    /**
     * Helper to check if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean = _currentUser.value != null

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(profile.id)
                .set(profile.encodeToMap()!!)
                .await()
            _currentUser.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
