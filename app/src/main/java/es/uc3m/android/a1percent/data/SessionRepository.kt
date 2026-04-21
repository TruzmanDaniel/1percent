package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import es.uc3m.android.a1percent.data.remote.ApiClient
import es.uc3m.android.a1percent.data.remote.dto.RegisterRequest
import es.uc3m.android.a1percent.data.remote.dto.UserDto
import es.uc3m.android.a1percent.data.remote.dto.LoginRequest

import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth

/**
 * Singleton repository to manage the active user session.
 * Responsible for AUTHENTICATION and IDENTIFICATION.
 */
object SessionRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }
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
                        "createdAt" to System.currentTimeMillis().toString()
                    )
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
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
                    val firebaseUser = auth.currentUser!!
                    // Read username in Firestore
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val username = doc.getString("username")
                                ?: email.substringBefore("@")
                            _currentUser.value = UserProfile(
                                id = firebaseUser.uid,
                                name = username,
                                email = firebaseUser.email ?: email,
                            )
                            onResult(true, null)
                        }
                        .addOnFailureListener {
                            // If firestore fails, we use email as username
                            _currentUser.value = UserProfile(
                                id = firebaseUser.uid,
                                name = email.substringBefore("@"),
                                email = firebaseUser.email ?: email,
                            )
                            onResult(true, null)
                        }
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

    // Shared state of the CURRENT authenticated user
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

    /**
     * Helper to check if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean = _currentUser.value != null
}
