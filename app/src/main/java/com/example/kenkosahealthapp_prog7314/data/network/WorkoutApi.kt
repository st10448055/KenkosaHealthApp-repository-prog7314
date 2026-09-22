package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit API interface used to communicate with the backend
// workout and exercise endpoints.
interface WorkoutApi {

    // GET /api/workouts/categories
    // Retrieves all available exercise categories from the backend.
    @GET("api/workouts/categories")
    suspend fun getCategories(): List<ExerciseCategoryDto>

    // GET /api/workouts?category={categoryId}
    // Retrieves all exercises belonging to the specified category.
    // The category ID is sent as a query parameter to the backend.
    @GET("api/workouts")
    suspend fun getExercisesByCategory(
        @Query("category") categoryId: Int
    ): List<ExerciseDto>
}

// ------------------ END OF FILE ------------------ //