package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import es.uc3m.android.a1percent.data.remote.ApiClient
import es.uc3m.android.a1percent.data.remote.dto.RegisterRequest
import es.uc3m.android.a1percent.data.remote.dto.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton repository to manage the active user session.
 * Responsible for AUTHENTICATION and IDENTIFICATION.
 */
object SessionRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }

    // Shared state of the CURRENT authenticated user
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        restoreCurrentUserIfAvailable()
    }

    private fun restoreCurrentUserIfAvailable() {
        val firebaseUser = auth.currentUser ?: return

        // Fallback user so the app can keep working immediately after restart
        _currentUser.value = UserProfile(
            id = firebaseUser.uid,
            name = firebaseUser.email?.substringBefore("@") ?: firebaseUser.uid,
            email = firebaseUser.email ?: ""
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")
                    ?: firebaseUser.email?.substringBefore("@")
                    ?: firebaseUser.uid

                _currentUser.value = UserProfile(
                    id = firebaseUser.uid,
                    name = username,
                    email = firebaseUser.email ?: "",
                    level = doc.getLong("level")?.toInt() ?: 1,
                    currentXp = doc.getLong("currentXp")?.toInt() ?: 0,
                    xpToNextLevel = doc.getLong("xpToNextLevel")?.toInt() ?: 100,
                    avatarUrl = doc.getString("avatarUrl"),
                    streakDays = doc.getLong("streakDays")?.toInt() ?: 0,
                    totalTasksCompleted = doc.getLong("totalTasksCompleted")?.toInt() ?: 0
                )
            }
    }
    /**
     * Receives the registration data and attempts to register with the API.
     *
     */
    suspend fun registerWithApi(
        email: String,
        password: String,
        username: String
    ): Result<UserDto> {
        return try {
            val response = ApiClient.apiService.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    username = username
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Error ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun registerWithFirebaseAndApi(
        email: String,
        password: String,
        username: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Create User on Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val firebaseUser = auth.currentUser!!
                    val userData = mapOf(
                        "id" to firebaseUser.uid,
                        "email" to email,
                        "username" to username,
                        "createdAt" to System.currentTimeMillis().toString(),
                        "level" to 1,
                        "currentXp" to 0,
                        "xpToNextLevel" to 100, // TODO Está bien asumir estos default? en que caso se usarian?
                        "avatarUrl" to null,
                        "streakDays" to 0,
                        "totalTasksCompleted" to 0
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                        .set(userData)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { onResult(false, it.message) }
                } else {
                    onResult(false, authTask.exception?.message)
                }
            }
    }

    ///**
    fun loginWithFirebase(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    restoreCurrentUserIfAvailable()
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
    //*/

    /**
    suspend fun loginWithApi(
        email: String,
        password: String
    ): Result<UserDto> {
        return try {
            val response = ApiClient.apiService.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Error ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception){
            Result.failure(e)
        }
    }
    */



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
}
