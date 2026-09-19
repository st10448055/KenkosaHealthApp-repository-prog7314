package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class MealEntryResponse(
    val mealId: String,
    val userId: String,
    val date: String,
    val mealType: String,
    val foodName: String,
    val servings: Double,
    val baseCalories: Double,
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val loggedAt: Long
)

@Serializable
data class CreateMealEntryRequest(
        val date: String,
        val mealType: String,
        val foodName: String,
        val servings: Double,

        // per serving values from the food search result, scaled by servings
        val baseCalories: Double,
        val baseProteinG: Double,
        val baseCarbsG: Double,
        val baseFatG: Double
        ){
    fun validate(): List<String>{
        val errors = mutableListOf<String>()
        if(date.isBlank()){
            errors.add("Date is required")
        }

        if(mealType !in listOf("breakfast", "lunch", "dinner", "snack" )){
            errors.add("mealType must be one of: breakfast, lunch, dinner, or snack")
        }

        if(foodName.isBlank()) errors.add("Food name is required")
        if (servings <= 0) errors.add("Servings must be greater than 0")
        return errors
    }

    @Serializable
    data class UpdateMealEntryRequest(
        val servings: Double
    ) {
        fun validate(): List<String> {
            val errors = mutableListOf<String>()
            if (servings <= 0) errors.add("Servings must be greater than 0")
            return errors
        }
    }


}

