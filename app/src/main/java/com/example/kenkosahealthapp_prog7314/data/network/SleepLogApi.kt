package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface SleepLogApi {
    @GET("api/sleep-log")
    suspend fun getSleep(): List<SleepLogDto>

    @GET("api/sleep-log/{id}")
    suspend fun getSleepRecord(@Path("id") sleepId: String): SleepLogDto

    @POST("api/sleep-log")
    suspend fun createSleep(@Body request: SaveSleepLogRequest): SleepLogDto

    @PUT("api/sleep-log/{id}")
    suspend fun updateSleep(
        @Path("id") sleepId: String,
        @Body request: SaveSleepLogRequest
    ): SleepLogDto

    @DELETE("api/sleep-log/{id}")
    suspend fun deleteSleep(@Path("id") sleepId: String)
}
