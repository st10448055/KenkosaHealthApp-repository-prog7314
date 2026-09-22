package com.kenkosa.backend.models

// Imports Kotlin Serialization so that this data class
// can be converted between a Kotlin object and JSON
// when communicating with the backend API.
import kotlinx.serialization.Serializable

// Allows this data class to be serialized and deserialized
// when sending and receiving food information through the API.
@Serializable
data class FoodItemResponse(

    // Unique identifier for the food item.
    val foodId: String,

    // Name of the food item.
    val name: String,

    // Optional barcode for the food item.
    // It is nullable because not every food item will have a barcode.
    val barcode: String? = null,

    // The serving size in grams that the nutritional values
    // are based on.
    val servingSizeG:Double, //the serving size the macros are based on in grams (g)

    // Number of calories contained in one serving.
    val calories: Double, // per serving

    // Amount of protein in grams per serving.
    val proteinG: Double,

    // Amount of carbohydrates in grams per serving.
    val carbsG: Double,

    // Amount of fat in grams per serving.
    val fatG: Double,

    // Identifies where the food and nutritional information
    // came from, such as OpenFoodFacts or USDA.
    val source: String      // "OpenFoodFact" and/or "USDA"
)

// ------------------ END OF FILE ------------------ //