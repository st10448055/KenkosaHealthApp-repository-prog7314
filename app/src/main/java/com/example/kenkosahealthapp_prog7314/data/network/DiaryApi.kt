package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiaryApi {
    @GET("api/diary")
    suspend fun getMeals(@Query("date") date: String? = null): List<MealEntryDto>

    @POST("api/diary")
    suspend fun createMeal(@Body request: CreateMealEntryRequest): MealEntryDto

    @DELETE("api/diary/{id}")
    suspend fun deleteMeal(@Path("id") mealId: String)

    @PATCH("api/diary/{id}")
    suspend fun updateMeal(@Path("id") mealId: String, @Body request: UpdateMealEntryRequest): MealEntryDto
}