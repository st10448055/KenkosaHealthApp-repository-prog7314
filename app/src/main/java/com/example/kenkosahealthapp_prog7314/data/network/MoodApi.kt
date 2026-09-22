package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

// Retrofit API interface used to communicate with the backend mood endpoints.
interface MoodApi {

    // GET /api/moods
    // Retrieves all mood entries belonging to the authenticated user.
    @GET("api/moods")
    suspend fun getMoods(): List<MoodEntryDto>

    // GET /api/moods/{id}
    // Retrieves a specific mood entry using its unique mood ID.
    @GET("api/moods/{id}")
    suspend fun getMood(
        @Path("id") moodId: String
    ): MoodEntryDto

    // POST /api/moods
    // Creates a new mood entry using the supplied mood information.
    @POST("api/moods")
    suspend fun createMood(
        @Body request: CreateMoodRequest
    ): MoodEntryDto

    // PATCH /api/moods/{id}
    // Updates an existing mood entry using its unique mood ID.
    @PATCH("api/moods/{id}")
    suspend fun updateMood(

        // ID of the mood entry that needs to be updated.
        @Path("id") moodId: String,

        // Updated mood information sent in the request body.
        @Body request: UpdateMoodRequest
    ): MoodEntryDto

    // DELETE /api/moods/{id}
    // Deletes the specified mood entry from the backend.
    @DELETE("api/moods/{id}")
    suspend fun deleteMood(

        // ID of the mood entry that should be deleted.
        @Path("id") moodId: String
    )
}

// ------------------ END OF FILE ------------------ //