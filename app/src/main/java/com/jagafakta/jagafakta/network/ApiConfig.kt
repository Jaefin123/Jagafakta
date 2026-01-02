// app/src/main/java/com/jagafakta/jagafakta/network/ApiConfig.kt
package com.jagafakta.jagafakta.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {

    // Android Emulator -> host PC gunakan 10.0.2.2
    private const val BASE_URL_BACKEND = "http://10.0.2.2:8000/"

    private val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofitBackend: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_BACKEND)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    // Satu-satunya service yang dipakai
    val apiService: ApiService by lazy {
        retrofitBackend.create(ApiService::class.java)
    }
}
