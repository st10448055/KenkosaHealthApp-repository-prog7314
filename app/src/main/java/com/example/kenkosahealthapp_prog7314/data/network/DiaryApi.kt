package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiaryApi {
    @GET("api/diary")
    suspend fun getMeals(@Query("date") date: String? = null): List<MealEntryDto>

    @POST("apu/diary")
    suspend fun createMeal(@Body request: CreateMealEntryRequest): MealEntryDto

    @DELETE("api/diary/{id}")
    suspend fun deleteMeal(@Path("id") mealId: String)
}