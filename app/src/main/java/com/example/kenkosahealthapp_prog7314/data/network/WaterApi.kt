package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

interface WaterApi {
    @GET("api/water")
    suspend fun getTodaysWater(@Query("date") date: String? = null): WaterIntakeDto

    @PATCH("api/water")
    suspend fun updateWater(@Body request: UpdateWaterRequest): WaterIntakeDto
}