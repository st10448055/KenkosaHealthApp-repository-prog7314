package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface GoalTrackingApi {
    @GET("api/goal-tracking")
    suspend fun getGoals(): List<TrackedGoalDto>

    @GET("api/goal-tracking/{id}")
    suspend fun getGoal(@Path("id") goalId: String): TrackedGoalDto

    @POST("api/goal-tracking")
    suspend fun createGoal(@Body request: SaveTrackedGoalRequest): TrackedGoalDto

    @PUT("api/goal-tracking/{id}")
    suspend fun updateGoal(
        @Path("id") goalId: String,
        @Body request: SaveTrackedGoalRequest
    ): TrackedGoalDto

    @PUT("api/goal-tracking/{id}/progress")
    suspend fun logProgress(
        @Path("id") goalId: String,
        @Body request: LogProgressRequest
    ): TrackedGoalDto

    @DELETE("api/goal-tracking/{id}")
    suspend fun deleteGoal(@Path("id") goalId: String)
}
