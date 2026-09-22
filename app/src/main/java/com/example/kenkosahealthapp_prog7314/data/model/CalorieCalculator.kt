package com.example.kenkosahealthapp_prog7314.data.model

import kotlin.math.roundToInt

object CalorieCalculator {

    /** Mifflin-St Jeor BMR, then scaled by activity level for a daily target (TDEE). */
    fun dailyCalorieTarget(metrics: UserMetrics): Int {
        val bmr = when (metrics.gender) {
            Gender.MALE ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age + 5
            Gender.FEMALE ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age - 161
            Gender.NON_BINARY, Gender.PREFER_NOT_TO_SAY ->
                10 * metrics.weightKg + 6.25 * metrics.heightCm - 5 * metrics.age - 78
        }

        val activityMultiplier = when (metrics.activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHT -> 1.375
            ActivityLevel.MODERATE -> 1.55
            ActivityLevel.VERY_ACTIVE -> 1.725
            ActivityLevel.EXTRA_ACTIVE -> 1.9
        }

        return (bmr * activityMultiplier).roundToInt()
    }
}