package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.MockData
import es.uc3m.android.a1percent.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import es.uc3m.android.a1percent.data.remote.ApiClient
import es.uc3m.android.a1percent.data.remote.dto.RegisterRequest
import es.uc3m.android.a1percent.data.remote.dto.UserDto

/**
 * Singleton repository to manage the active user session.
 * Responsible for AUTHENTICATION and IDENTIFICATION.
 */
object SessionRepository {
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
