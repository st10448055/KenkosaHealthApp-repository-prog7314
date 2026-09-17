package com.example.kenkosahealthapp_prog7314.data.model

data class UserSettings(
    val userId: String = "",
    val unitSystem: String = "Metric",
    val language: String = "en",
    val appearanceMode: String = "System",
    val hapticsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val subscriptionStatus: String = "Free"
) {
    constructor() : this(userId = "")

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "unitSystem" to unitSystem,
        "language" to language,
        "appearanceMode" to appearanceMode,
        "hapticsEnabled" to hapticsEnabled,
        "notificationsEnabled" to notificationsEnabled,
        "subscriptionStatus" to subscriptionStatus
    )
}