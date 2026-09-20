package com.kenkosa.backend.models

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneOffset

private val REMINDER_TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")


@Serializable
enum class TrackedGoalCategory { STEPS, WATER, SLEEP, EXERCISE, CUSTOM }

@Serializable
enum class TrackedGoalFrequency { DAILY, WEEKLY, MONTHLY }

@Serializable
data class TrackingErrorResponse(val errors: List<String>)


@Serializable
data class TrackedGoalResponse(
    val goalId: String,
    val userId: String,
    val category: TrackedGoalCategory,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: TrackedGoalFrequency,
    val remindersEnabled: Boolean,
    val reminderTime: String,           // "HH:mm" (24h)
    val progressLog: Map<String, Double>,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SaveTrackedGoalRequest(
    val category: TrackedGoalCategory,
    val name: String,
    val targetValue: Double,
    val unit: String,
    val frequency: TrackedGoalFrequency,
    val remindersEnabled: Boolean = false,
    val reminderTime: String = "06:00"
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Goal name cannot be empty")
        if (name.length > 60) errors.add("Goal name must be 60 characters or fewer")
        if (targetValue <= 0) errors.add("Target value must be greater than 0")
        if (targetValue > 1_000_000) errors.add("Target value is unrealistically large")
        if (category == TrackedGoalCategory.SLEEP && targetValue > 24) errors.add("Sleep target can't exceed 24 hours")
        if (unit.isBlank()) errors.add("Unit is required")
        if (unit.length > 20) errors.add("Unit must be 20 characters or fewer")
        if (!REMINDER_TIME_REGEX.matches(reminderTime)) errors.add("Reminder time must be in HH:mm format")
        return errors
    }
}


@Serializable
data class LogProgressRequest(
    val date: String,      // "yyyy-MM-dd"
    val value: Double
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
        if (parsed == null) {
            errors.add("Date must be in yyyy-MM-dd format")
        } else if (parsed.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))) {
            // One day of slack covers phones that are ahead of the server's time zone.
            errors.add("Progress can't be logged for a future date")
        }
        if (value < 0) errors.add("Progress cannot be negative")
        if (value > 1_000_000) errors.add("Progress value is unrealistically large")
        return errors
    }
}