package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface GoalApi {
    @GET("api/goals")
    suspend fun getGoals(): List<GoalDto>

    @POST("api/goals")
    suspend fun createGoal(@Body request: CreateGoalRequest): GoalDto

    @PATCH("api/goals/{id}")
    suspend fun updateGoal(@Path("id") goalId: String, @Body request: UpdateGoalRequest): GoalDto

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") goalId: String)
}