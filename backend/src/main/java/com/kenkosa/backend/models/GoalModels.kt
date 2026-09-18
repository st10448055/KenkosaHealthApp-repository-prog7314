package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
enum class GoalCategory { STEPS, WATER, SLEEP, EXERCISE, CUSTOM }

@Serializable
enum class GoalFrequency { DAILY, WEEKLY, MONTHLY }

@Serializable
data class GoalResponse(
    val goalId: String,
    val userId: String,
    val category: GoalCategory,
    val name: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val frequency: GoalFrequency,
    val remindersEnabled: Boolean,
    val weeklyStreak: List<Boolean>, // 7 entries, Mon..Sun
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateGoalRequest(
    val category: GoalCategory,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: GoalFrequency,
    val remindersEnabled: Boolean = false
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Goal name cannot be empty")
        if (name.length > 60) errors.add("Goal name must be 60 characters or fewer")
        if (targetValue <= 0) errors.add("Target value must be greater than 0")
        if (targetValue > 1_000_000) errors.add("Target value is unrealistically large")
        if (unit.isBlank()) errors.add("Unit is required")
        return errors
    }
}

@Serializable
data class UpdateGoalRequest(
    val currentValue: Double? = null,
    val targetValue: Double? = null,
    val name: String? = null,
    val remindersEnabled: Boolean? = null
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        targetValue?.let { if (it <= 0) errors.add("Target value must be greater than 0") }
        currentValue?.let { if (it < 0) errors.add("Current value cannot be negative") }
        name?.let { if (it.isBlank()) errors.add("Goal name cannot be empty") }
        return errors
    }
}

@Serializable
data class ErrorResponse(val errors: List<String>)