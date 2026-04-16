package es.uc3m.android.a1percent.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // 10.0.2.2: my PC from Android view, 5001: port of Firebase Functions
    private const val BASE_URL =
        "http://10.0.2.2:5001/uc3m-it-2026-16504-g04-96/us-central1/app/"

    // by lazy: only initialized when first accessed, and then cached for future use
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}