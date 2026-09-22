package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface WorkoutApi {
    @GET("api/workouts/categories")
    suspend fun getCategories(): List<ExerciseCategoryDto>

    @GET("api/workouts")
    suspend fun getExercisesByCategory(@Query("category") categoryId: Int): List<ExerciseDto>
}