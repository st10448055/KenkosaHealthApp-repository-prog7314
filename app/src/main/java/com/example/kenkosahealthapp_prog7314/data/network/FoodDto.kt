package com.example.kenkosahealthapp_prog7314.data.network

data class FoodItemDto(
    val foodId: String,
    val name: String,
    val barcode: String,
    val servingSizeG: Double,
    val calories: Double,   // per serving
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: String      //OpenFoodFacts or USDA
)