package com.example.kenkosahealthapp_prog7314.data.model

import kotlin.math.roundToInt

// Object responsible for calculating a user's estimated daily calorie target.
object CalorieCalculator {

    /// Calculates the user's estimated daily calorie requirement.
    // The calculation first determines the Basal Metabolic Rate (BMR)
    // using the Mifflin-St Jeor equation, then multiplies it by the
    // user's activity level to estimate their Total Daily Energy Expenditure (TDEE).
    fun dailyCalorieTarget(metrics: UserMetrics): Int {
        // Calculate BMR based on the user's gender, weight, height, and age.
        val bmr = when (metrics.gender) {
            // Mifflin-St Jeor equation for males.
            Gender.MALE ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age + 5
            // Mifflin-St Jeor equation for females.
            Gender.FEMALE ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age - 161
            // Uses a midpoint adjustment for non-binary users
            // and users who prefer not to provide their gender.
            Gender.NON_BINARY, Gender.PREFER_NOT_TO_SAY ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age - 78
        }

        // Select an activity multiplier based on the user's
        // usual level of physical activity.
        val activityMultiplier = when (metrics.activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHT -> 1.375
            ActivityLevel.MODERATE -> 1.55
            ActivityLevel.VERY_ACTIVE -> 1.725
            ActivityLevel.EXTRA_ACTIVE -> 1.9
        }
        // Multiply the BMR by the activity multiplier to estimate
        // the user's daily calorie requirement (TDEE).
        // roundToInt() converts the calculated decimal value to a whole number.
        return (bmr * activityMultiplier).roundToInt()
    }
}

// ------------------ END OF FILE ------------------ //