package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Defines the Retrofit API endpoints used to manage
// the user's food diary and meal entries.
interface DiaryApi {

    // Sends a GET request to /api/diary.
    // The optional date parameter can be used to retrieve
    // meals for a specific date.
    // If date is null, the backend can return the user's
    // available diary entries without filtering by date.
    @GET("api/diary")
    suspend fun getMeals(
        @Query("date") date: String? = null
    ): List<MealEntryDto>

    // Sends a POST request to /api/diary.
    // Creates and stores a new meal entry.
    // The request contains the meal and nutritional information.
    @POST("api/diary")
    suspend fun createMeal(
        @Body request: CreateMealEntryRequest
    ): MealEntryDto

    // Sends a DELETE request to /api/diary/{id}.
    // Deletes the meal entry identified by its meal ID.
    @DELETE("api/diary/{id}")
    suspend fun deleteMeal(
        @Path("id") mealId: String
    )

    // Sends a PATCH request to /api/diary/{id}.
    // Updates an existing meal entry using its ID.
    // The request currently contains the updated number of servings,
    // which the backend can use to recalculate the nutritional totals.
    @PATCH("api/diary/{id}")
    suspend fun updateMeal(
        @Path("id") mealId: String,
        @Body request: UpdateMealEntryRequest
    ): MealEntryDto
}

// ------------------ END OF FILE ------------------ //