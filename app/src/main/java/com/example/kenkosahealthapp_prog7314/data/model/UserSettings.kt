package com.example.kenkosahealthapp_prog7314.data.model

data class UserSettings(
    val userId: String = "",
    val unitSystem: String = "Metric",
    val language: String = "en",
    val appearanceMode: String = "Light",
    val hapticsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val subscriptionStatus: String = "Free",
    // Health goals shown on the Settings screen
    val dailyStepGoal: Int = 10_000,
    val calorieTarget: Int = 2_500,
    val sleepTargetMinutes: Int = 450 // 7h 30m
) {
    constructor() : this(userId = "")

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "unitSystem" to unitSystem,
        "language" to language,
        "appearanceMode" to appearanceMode,
        "hapticsEnabled" to hapticsEnabled,
        "notificationsEnabled" to notificationsEnabled,
        "subscriptionStatus" to subscriptionStatus,
        "dailyStepGoal" to dailyStepGoal,
        "calorieTarget" to calorieTarget,
        "sleepTargetMinutes" to sleepTargetMinutes
    )
}