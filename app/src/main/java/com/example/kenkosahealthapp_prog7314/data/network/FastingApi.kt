package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface FastingApi {
    @GET("api/fasts")
    suspend fun getFasts(): List<FastDto>

    @GET("api/fasts/{id}")
    suspend fun getFast(@Path("id") fastId: String): FastDto

    @POST("api/fasts")
    suspend fun createFast(@Body request: CreateFastRequest): FastDto

    @PATCH("api/fasts/{id}")
    suspend fun updateFast(
        @Path("id") fastId: String,
        @Body request: UpdateFastRequest
    ): FastDto

    @DELETE("api/fasts/{id}")
    suspend fun deleteFast(@Path("id") fastId: String)
}