package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// Response model for an exercise category.
@Serializable
data class ExerciseCategoryResponse(
    val id: Int,
    val name: String
)

// Response model containing the details of an exercise.
@Serializable
data class ExerciseResponse(
    val id: Int,
    val name: String,
    val description: String,
    val categoryId: Int,
    val categoryName: String,
    val equipment: List<String> = emptyList(), // Equipment needed for the exercise.
    val muscles: List<String> = emptyList()    // Muscle groups targeted by the exercise.
)

// ------------------ END OF FILE ------------------ //