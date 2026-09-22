package com.example.kenkosahealthapp_prog7314.data.network

// Data Transfer Object (DTO) representing a meal entry
// returned by the backend API.
data class MealEntryDto(

    // Unique identifier for the meal entry.
    val mealId: String,

    // Unique identifier of the user who logged the meal.
    val userId: String,

    // Date on which the meal was consumed/logged.
    val date: String,

    // Type of meal, for example breakfast, lunch, dinner, or snack.
    val mealType: String,

    // Name of the food that was logged.
    val foodName: String,

    // Number of servings consumed.
    val servings: Double,

    // Calories contained in one serving before scaling.
    val baseCalories: Double,

    // Protein contained in one serving before scaling, measured in grams.
    val baseProteinG: Double,

    // Carbohydrates contained in one serving before scaling, measured in grams.
    val baseCarbsG: Double,

    // Fat contained in one serving before scaling, measured in grams.
    val baseFatG: Double,

    // Total calories for the logged servings.
    // The backend has already multiplied the base calories by the servings.
    val calories: Double,

    // Total protein for the logged servings, in grams.
    val proteinG: Double,

    // Total carbohydrates for the logged servings, in grams.
    val carbsG: Double,

    // Total fat for the logged servings, in grams.
    val fatG: Double,

    // Timestamp indicating when the meal was logged.
    val loggedAt: Long
)

// Request object used when creating a new meal entry.
// These values are sent from the Android application to the backend.
data class CreateMealEntryRequest(

    // Date on which the meal was consumed/logged.
    val date: String,

    // Type of meal being logged.
    val mealType: String,

    // Name of the food being logged.
    val foodName: String,

    // Number of servings consumed.
    val servings: Double,

    // Calories per serving.
    // These values come directly from FoodItemDto.
    val baseCalories: Double,

    // Protein per serving, in grams.
    // These values come directly from FoodItemDto.
    val baseProteinG: Double,

    // Carbohydrates per serving, in grams.
    // These values come directly from FoodItemDto.
    val baseCarbsG: Double,

    // Fat per serving, in grams.
    // These values come directly from FoodItemDto.
    val baseFatG: Double
)

// Request object used when updating an existing meal entry.
data class UpdateMealEntryRequest(

    // New number of servings for the meal.
    // The backend can use this value to recalculate
    // the total nutritional information.
    val servings: Double
)

// ------------------ END OF FILE ------------------ //