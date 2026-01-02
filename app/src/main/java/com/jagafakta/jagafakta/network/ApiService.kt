// app/src/main/java/com/jagafakta/jagafakta/network/ApiService.kt
package com.jagafakta.jagafakta.network

import com.jagafakta.jagafakta.model.AuthModels
import com.jagafakta.jagafakta.model.ScanHistory
import com.jagafakta.jagafakta.model.TextPredictRequest
import com.jagafakta.jagafakta.model.PredictResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // 1) AUTH
    @POST("auth/login")
    suspend fun login(
        @Body req: AuthModels.LoginRequest
    ): AuthModels.LoginResponse

    @POST("auth/register")
    suspend fun register(
        @Body req: AuthModels.RegisterRequest
    ): AuthModels.RegisterResponse

    // 2) SCAN HISTORY
    @POST("scan-history")
    suspend fun postHistory(
        @Body req: ScanHistory.ScanHistoryRequest
    ): ScanHistory.BaseResponse

    @GET("scan-history/{userId}")
    suspend fun getHistory(
        @Path("userId") userId: String
    ): List<ScanHistory.ScanHistoryItem>


    @POST("predict")
    suspend fun predict(
        @Body req: TextPredictRequest
    ): PredictResponse
}
