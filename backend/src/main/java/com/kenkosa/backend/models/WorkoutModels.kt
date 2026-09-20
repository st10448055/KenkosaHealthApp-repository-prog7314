package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

    @Serializable
    data class ExerciseCategoryResponse(
        val id: Int,
        val name: String
    )

    @Serializable
    data class ExerciseResponse(
        val id: Int,
        val name: String,
        val description: String,
        val categoryId: Int,
        val categoryName: String,
        val equipment: List<String> = emptyList(),
        val muscles: List<String> = emptyList())

