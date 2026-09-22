package com.example.kenkosahealthapp_prog7314.data.network

// Data Transfer Object (DTO) representing a food item
// returned by the backend food API.
data class FoodItemDto(

    // Unique identifier for the food item.
    val foodId: String,

    // Name of the food item.
    val name: String,

    // Barcode associated with the food item.
    val barcode: String,

    // Serving size of the food item in grams.
    val servingSizeG: Double,

    // Number of calories contained in one serving.
    val calories: Double,

    // Amount of protein in one serving, measured in grams.
    val proteinG: Double,

    // Amount of carbohydrates in one serving, measured in grams.
    val carbsG: Double,

    // Amount of fat in one serving, measured in grams.
    val fatG: Double,

    // Identifies the external data source used for the food item.
    // Examples include OpenFoodFacts or USDA.
    val source: String
)

// ------------------ END OF FILE ------------------ //