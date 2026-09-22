package com.example.kenkosahealthapp_prog7314.data.model

// Stores the user's application and health-related settings.
data class UserSettings(

    // Unique identifier for the user.
    val userId: String = "",

    // Measurement system used by the user.
    // "Metric" uses units such as kilometres, kilograms, and centimetres.
    val unitSystem: String = "Metric",

    // Language selected for the application.
    // "en" represents English.
    val language: String = "en",

    // Controls the application's appearance mode.
    // The default mode is Light.
    val appearanceMode: String = "Light",

    // Determines whether haptic feedback is enabled.
    val hapticsEnabled: Boolean = true,

    // Determines whether application notifications are enabled.
    val notificationsEnabled: Boolean = true,

    // Stores the user's current subscription status.
    // New users are set to the Free plan by default.
    val subscriptionStatus: String = "Free",

    // Daily number of steps the user aims to complete.
    // The default goal is 10,000 steps.
    val dailyStepGoal: Int = 10_000,

    // Daily calorie target for the user.
    // The default target is 2,500 calories.
    val calorieTarget: Int = 2_500,

    // Daily sleep target stored in minutes.
    // 450 minutes represents 7 hours and 30 minutes.
    val sleepTargetMinutes: Int = 450
) {

    // Empty constructor required by Firebase Firestore
    // when converting a Firestore document into a UserSettings object.
    constructor() : this(userId = "")

    // Converts the UserSettings object into a Map.
    // This allows the settings to be saved to or updated in Firestore.
    fun toMap(): Map<String, Any?> = mapOf(

        // Store the user's unique ID.
        "userId" to userId,

        // Store the selected measurement system.
        "unitSystem" to unitSystem,

        // Store the selected application language.
        "language" to language,

        // Store the selected appearance mode.
        "appearanceMode" to appearanceMode,

        // Store whether haptic feedback is enabled.
        "hapticsEnabled" to hapticsEnabled,

        // Store whether notifications are enabled.
        "notificationsEnabled" to notificationsEnabled,

        // Store the user's subscription status.
        "subscriptionStatus" to subscriptionStatus,

        // Store the user's daily step goal.
        "dailyStepGoal" to dailyStepGoal,

        // Store the user's daily calorie target.
        "calorieTarget" to calorieTarget,

        // Store the user's daily sleep target in minutes.
        "sleepTargetMinutes" to sleepTargetMinutes
    )
}

// ------------------ END OF FILE ------------------ //