package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SleepApi {
    @GET("api/sleep")
    suspend fun getSleep(
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null
    ): List<SleepRecordDto>

    @POST("api/sleep")
    suspend fun createSleep(@Body request: CreateSleepRequest): SleepRecordDto
}