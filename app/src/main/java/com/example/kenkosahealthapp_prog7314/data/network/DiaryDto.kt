package com.example.kenkosahealthapp_prog7314.data.network

data class MealEntryDto (
    val mealId: String,
    val userId: String,
    val date: String,
    val mealType: String,
    val foodName: String,
    val servings: Double,
    val calories: Double,      // already scaled by servings, from backend
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val loggedAt: Long
)

data class CreateMealEntryRequest(
    val date: String,
    val mealType: String,
    val foodName: String,
    val servings: Double,
    val baseCalories: Double,   // per-serving values, straight from FoodItemDto
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double
)