package com.kenkosa.backend.models

// Imports Kotlin Serialization so that the classes in this file
// can be converted between Kotlin objects and JSON when
// communicating with the backend API.
import kotlinx.serialization.Serializable

// Used to validate and work with dates without including a time.
import java.time.LocalDate

// Used to work with UTC time when checking whether a date
// is too far in the future.
import java.time.ZoneOffset

// Regular expression used to check that reminder times follow
// the 24-hour HH:mm format, for example "06:00" or "18:30".
private val REMINDER_TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")


// Represents the different types of goals that the user can track.
// The CUSTOM option allows a goal that does not fit into the
// predefined health categories.
@Serializable
enum class TrackedGoalCategory { STEPS, WATER, SLEEP, EXERCISE, CUSTOM }


// Represents how often a goal should be tracked or repeated.
@Serializable
enum class TrackedGoalFrequency { DAILY, WEEKLY, MONTHLY }


// Represents an error response from the backend.
// The list allows multiple validation errors to be returned
// at the same time.
@Serializable
data class TrackingErrorResponse(val errors: List<String>)


// Represents a tracked goal returned by the backend.
@Serializable
data class TrackedGoalResponse(

    // Unique ID for the tracked goal.
    val goalId: String,

    // Identifies the user who owns the goal.
    val userId: String,

    // Category that the goal belongs to, such as steps or sleep.
    val category: TrackedGoalCategory,

    // Name given to the goal by the user.
    val name: String,

    // The value the user is trying to achieve.
    val targetValue: Double,

    // Unit used to measure the goal, such as steps, litres, or hours.
    val unit: String,

    // How often the goal is tracked.
    val frequency: TrackedGoalFrequency,

    // Determines whether reminders have been enabled for this goal.
    val remindersEnabled: Boolean,

    // Time at which the reminder should be sent.
    // The expected format is 24-hour "HH:mm".
    val reminderTime: String,           // "HH:mm" (24h)

    // Stores the user's progress using a date as the key
    // and the progress value as the value.
    val progressLog: Map<String, Double>,

    // Records when the goal was originally created.
    val createdAt: Long,

    // Records when the goal was last updated.
    val updatedAt: Long
)


// Represents the information sent to the backend
// when creating or saving a tracked goal.
@Serializable
data class SaveTrackedGoalRequest(

    // Category selected for the goal.
    val category: TrackedGoalCategory,

    // Name of the goal.
    val name: String,

    // Target amount the user wants to reach.
    val targetValue: Double,

    // Unit used to measure the target.
    val unit: String,

    // Frequency at which the goal should be tracked.
    val frequency: TrackedGoalFrequency,

    // Controls whether reminders are enabled.
    // Reminders are disabled by default.
    val remindersEnabled: Boolean = false,

    // Default reminder time if the user does not provide one.
    val reminderTime: String = "06:00"
) {

    // Validates the goal information before it is saved.
    // All validation errors are collected and returned as a list.
    fun validate(): List<String> {

        // Stores any validation errors that are found.
        val errors = mutableListOf<String>()

        // A goal must have a name.
        if (name.isBlank()) errors.add("Goal name cannot be empty")

        // Prevents goal names from becoming unnecessarily long.
        if (name.length > 60) errors.add("Goal name must be 60 characters or fewer")

        // The target must be greater than zero.
        if (targetValue <= 0) errors.add("Target value must be greater than 0")

        // Prevents an extremely large target value from being entered.
        if (targetValue > 1_000_000) errors.add("Target value is unrealistically large")

        // A sleep target cannot be more than 24 hours.
        if (category == TrackedGoalCategory.SLEEP && targetValue > 24) errors.add("Sleep target can't exceed 24 hours")

        // A unit is required so that the target value has meaning.
        if (unit.isBlank()) errors.add("Unit is required")

        // Prevents the unit name from being too long.
        if (unit.length > 20) errors.add("Unit must be 20 characters or fewer")

        // Checks that the reminder time follows the required HH:mm format.
        if (!REMINDER_TIME_REGEX.matches(reminderTime)) errors.add("Reminder time must be in HH:mm format")

        // Returns all validation errors.
        // An empty list means the goal passed all validation checks.
        return errors
    }
}


// Represents the information sent to the backend
// when the user records progress towards a tracked goal.
@Serializable
data class LogProgressRequest(

    // Date on which the progress was recorded.
    // The expected format is "yyyy-MM-dd".
    val date: String,      // "yyyy-MM-dd"

    // Amount of progress recorded for that date.
    val value: Double
) {

    // Validates the date and progress value before it is logged.
    fun validate(): List<String> {

        // Stores any validation errors that are found.
        val errors = mutableListOf<String>()

        // Attempts to convert the supplied text into a LocalDate.
        // If the conversion fails, the date is not in the correct format.
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()

        if (parsed == null) {

            // The date could not be parsed, so the required format
            // was not followed.
            errors.add("Date must be in yyyy-MM-dd format")

        } else if (parsed.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))) {

            // Prevents users from logging progress too far into the future.
            // One day of flexibility is allowed because the phone and
            // server may be using slightly different time zones.
            // One day of slack covers phones that are ahead of the server's time zone.
            errors.add("Progress can't be logged for a future date")
        }

        // Progress cannot be a negative number.
        if (value < 0) errors.add("Progress cannot be negative")

        // Prevents an unrealistically large progress value.
        if (value > 1_000_000) errors.add("Progress value is unrealistically large")

        // Returns all validation errors.
        // An empty list means the progress entry is valid.
        return errors
    }
}


// ------------------ END OF FILE ------------------ //