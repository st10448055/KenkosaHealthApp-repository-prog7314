package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// This data class represents a meal entry returned by the backend.
// @Serializable allows Kotlin Serialization to convert this object
// to and from JSON when communicating with the API.
@Serializable
data class MealEntryResponse(

    // Unique ID used to identify the meal entry.
    val mealId: String,

    // ID of the user who logged the meal.
    val userId: String,

    // The date on which the meal was logged.
    val date: String,

    // The category of the meal, for example breakfast, lunch, dinner, or snack.
    val mealType: String,

    // Name of the food that was added to the meal.
    val foodName: String,

    // Number of servings consumed by the user.
    val servings: Double,

    // Base nutritional information for the food.
    val baseCalories: Double,
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double,

    // The total nutritional values for the logged number of servings.
    // These values can therefore differ from the base values
    // when the user logs more or less than one serving.
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,

    // Stores the time at which the meal was logged.
    val loggedAt: Long
)

// This data class represents the information sent to the backend
// when a user creates a new meal entry.
@Serializable
data class CreateMealEntryRequest(
    // Date for which the meal is being recorded.
    val date: String,

    // Type/category of the meal being logged.
    val mealType: String,

    // Name of the food being added.
    val foodName: String,

    // Number of servings entered by the user.
    val servings: Double,

    // Per-serving nutritional values obtained from the food search result.
    // These values are used together with the number of servings
    // to calculate the total nutritional information for the meal.
    val baseCalories: Double,
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double
){

    // This function checks whether the information supplied
    // when creating a meal entry is valid.
    // It returns a list containing any validation errors found.
    fun validate(): List<String>{

        // A mutable list is used so that multiple validation errors
        // can be collected before the request is rejected.
        val errors = mutableListOf<String>()

        // A meal cannot be created without a date.
        if(date.isBlank()){
            errors.add("Date is required")
        }

        // The meal type must match one of the four supported categories.
        if(mealType !in listOf("breakfast", "lunch", "dinner", "snack" )){
            errors.add("mealType must be one of: breakfast, lunch, dinner, or snack")
        }

        // The food name is required because the meal needs to identify
        // what food the user consumed.
        if(foodName.isBlank()) errors.add("Food name is required")

        // A serving amount must be greater than zero.
        // Zero or negative servings would not represent a valid meal entry.
        if (servings <= 0) errors.add("Servings must be greater than 0")

        // Returns all validation errors found.
        // An empty list means that all of the checks passed.
        return errors
    }

    // This data class represents the information sent when
    // an existing meal entry needs to be updated.
    @Serializable
    data class UpdateMealEntryRequest(

        // The new number of servings for the existing meal.
        val servings: Double
    ) {

        // Validates the new serving amount before the meal is updated.
        fun validate(): List<String> {

            // Stores any validation errors that are found.
            val errors = mutableListOf<String>()

            // The updated serving amount must be greater than zero.
            if (servings <= 0) errors.add("Servings must be greater than 0")

            // Returns the validation errors.
            // An empty list means the updated value is valid.
            return errors
        }
    }


}
// ------------------ END OF FILE ------------------ //