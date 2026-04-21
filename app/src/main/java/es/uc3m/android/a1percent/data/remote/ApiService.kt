package es.uc3m.android.a1percent.data.remote

import es.uc3m.android.a1percent.data.remote.dto.RegisterRequest
import es.uc3m.android.a1percent.data.remote.dto.LoginRequest
import es.uc3m.android.a1percent.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("register")
    // suspend fun executes async and makes HTTP calls without blocking the main thread
    suspend fun register(
        // converts Kotlin data class to JSON and retrofits returns Response and UserDto
        @Body request: RegisterRequest
    ): Response<UserDto>

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<UserDto>

    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>
}