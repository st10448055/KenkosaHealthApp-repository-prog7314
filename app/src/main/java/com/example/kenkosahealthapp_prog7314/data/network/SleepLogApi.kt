package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

// Retrofit API interface for communicating with the backend sleep-log endpoints.
interface SleepLogApi {

    // GET /api/sleep-log
    // Retrieves all sleep records belonging to the authenticated user.
    @GET("api/sleep-log")
    suspend fun getSleep(): List<SleepLogDto>

    // GET /api/sleep-log/{id}
    // Retrieves a specific sleep record using its unique sleep ID.
    @GET("api/sleep-log/{id}")
    suspend fun getSleepRecord(
        @Path("id") sleepId: String
    ): SleepLogDto

    // POST /api/sleep-log
    // Creates a new sleep record using the supplied sleep information.
    @POST("api/sleep-log")
    suspend fun createSleep(
        @Body request: SaveSleepLogRequest
    ): SleepLogDto

    // PUT /api/sleep-log/{id}
    // Updates an existing sleep record using its unique sleep ID.
    @PUT("api/sleep-log/{id}")
    suspend fun updateSleep(
        // ID of the sleep record that needs to be updated.
        @Path("id") sleepId: String,

        // Updated sleep information sent in the request body.
        @Body request: SaveSleepLogRequest
    ): SleepLogDto

    // DELETE /api/sleep-log/{id}
    // Deletes the specified sleep record from the backend.
    @DELETE("api/sleep-log/{id}")
    suspend fun deleteSleep(
        // ID of the sleep record that should be deleted.
        @Path("id") sleepId: String
    )
}

// ------------------ END OF FILE ------------------ //