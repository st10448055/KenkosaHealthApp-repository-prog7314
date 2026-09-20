package com.example.kenkosahealthapp_prog7314.data.network


data class TrackedGoalDto(
    val goalId: String,
    val userId: String,
    val category: String,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: String,
    val remindersEnabled: Boolean,
    val reminderTime: String?,
    val progressLog: Map<String, Double>?,
    val createdAt: Long,
    val updatedAt: Long
)

/** Used to create a goal and to edit one. */
data class SaveTrackedGoalRequest(
    val category: String,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: String,
    val remindersEnabled: Boolean,
    val reminderTime: String
)

data class LogProgressRequest(
    val date: String,
    val value: Double
)
