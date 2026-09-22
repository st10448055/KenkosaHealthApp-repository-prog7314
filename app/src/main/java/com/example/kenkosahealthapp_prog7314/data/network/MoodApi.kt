package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface MoodApi {
    @GET("api/moods")
    suspend fun getMoods(): List<MoodEntryDto>

    @GET("api/moods/{id}")
    suspend fun getMood(@Path("id") moodId: String): MoodEntryDto

    @POST("api/moods")
    suspend fun createMood(@Body request: CreateMoodRequest): MoodEntryDto

    @PATCH("api/moods/{id}")
    suspend fun updateMood(
        @Path("id") moodId: String,
        @Body request: UpdateMoodRequest
    ): MoodEntryDto

    @DELETE("api/moods/{id}")
    suspend fun deleteMood(@Path("id") moodId: String)
}
