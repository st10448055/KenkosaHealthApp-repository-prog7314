package com.example.kenkosahealthapp_prog7314.data.network

data class GoalDto(
    val goalId: String,
    val userId: String,
    val category: String,
    val name: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val frequency: String,
    val remindersEnabled: Boolean,
    val weeklyStreak: List<Boolean>,
    val createdAt: Long,
    val updatedAt: Long
)

data class CreateGoalRequest(
    val category: String,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: String,
    val remindersEnabled: Boolean = false
)

data class UpdateGoalRequest(
    val currentValue: Double? = null,
    val targetValue: Double? = null,
    val name: String? = null,
    val remindersEnabled: Boolean? = null
)