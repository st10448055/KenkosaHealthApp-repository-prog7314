package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

// Retrofit API interface for communicating with the backend water endpoints.
interface WaterApi {

    // GET /api/water
    // Retrieves the user's water intake for a specific date.
    // If no date is supplied, the backend can use the current date.
    @GET("api/water")
    suspend fun getTodaysWater(
        @Query("date") date: String? = null
    ): WaterIntakeDto

    // PATCH /api/water
    // Updates the user's water intake using the data provided in the request body.
    // Returns the updated water intake record.
    @PATCH("api/water")
    suspend fun updateWater(
        @Body request: UpdateWaterRequest
    ): WaterIntakeDto
}

// ------------------ END OF FILE ------------------ //