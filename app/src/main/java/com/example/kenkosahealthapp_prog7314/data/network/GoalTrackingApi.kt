package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

// Defines the Retrofit API endpoints used to manage
// the user's tracked health goals and their progress.
interface GoalTrackingApi {

    // Sends a GET request to /api/goal-tracking.
    // Retrieves all tracked goals belonging to the
    // authenticated user.
    @GET("api/goal-tracking")
    suspend fun getGoals(): List<TrackedGoalDto>

    // Sends a GET request to /api/goal-tracking/{id}.
    // Retrieves a specific tracked goal using its ID.
    @GET("api/goal-tracking/{id}")
    suspend fun getGoal(
        @Path("id") goalId: String
    ): TrackedGoalDto

    // Sends a POST request to /api/goal-tracking.
    // Creates a new tracked goal using the supplied
    // goal information.
    @POST("api/goal-tracking")
    suspend fun createGoal(
        @Body request: SaveTrackedGoalRequest
    ): TrackedGoalDto

    // Sends a PUT request to /api/goal-tracking/{id}.
    // Updates an existing tracked goal using its ID
    // and the supplied goal information.
    @PUT("api/goal-tracking/{id}")
    suspend fun updateGoal(
        @Path("id") goalId: String,
        @Body request: SaveTrackedGoalRequest
    ): TrackedGoalDto

    // Sends a PUT request to /api/goal-tracking/{id}/progress.
    // Records progress for a specific tracked goal.
    // The request contains the date and the amount of
    // progress recorded for that date.
    @PUT("api/goal-tracking/{id}/progress")
    suspend fun logProgress(
        @Path("id") goalId: String,
        @Body request: LogProgressRequest
    ): TrackedGoalDto

    // Sends a DELETE request to /api/goal-tracking/{id}.
    // Deletes the specified tracked goal.
    @DELETE("api/goal-tracking/{id}")
    suspend fun deleteGoal(
        @Path("id") goalId: String
    )
}

// ------------------ END OF FILE ------------------ //