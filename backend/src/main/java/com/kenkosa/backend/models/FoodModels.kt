package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class FoodItemResponse(
    val foodId: String,
    val name: String,
    val barcode: String? = null,
    val servingSizeG:Double, //the serving size the macros are based on in grams (g)
    val calories: Double, // per serving
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: String      // "OpenFoodFact" and/or "USDA"
)
