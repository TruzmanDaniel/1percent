package es.uc3m.android.a1percent.data.remote.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)