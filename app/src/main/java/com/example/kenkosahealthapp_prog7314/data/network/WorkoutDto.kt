package com.example.kenkosahealthapp_prog7314.data.network

data class ExerciseCategoryDto(
    val id: Int,
    val name: String
)

data class ExerciseDto(
    val id: Int,
    val name: String,
    val description: String,
    val categoryId: Int,
    val categoryName: String,
    val equipment: List<String> = emptyList(),
    val muscles: List<String> = emptyList()
)