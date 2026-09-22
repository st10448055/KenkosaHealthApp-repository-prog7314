package com.kenkosa.backend.models

// Imports Kotlin Serialization so that the data classes in this file
// can be converted between Kotlin objects and JSON when communicating
// with the API.
import kotlinx.serialization.Serializable

// Allows a small difference between the phone's clock and the server's clock.
// 5 minutes is used as the maximum accepted clock difference.
private const val MAX_SKEW_MS = 5 * 60 * 1000L

// Defines the maximum amount of time that a fast can last.
// 7 days is converted into milliseconds because timestamps are stored in milliseconds.
private const val MAX_FAST_MS = 7 * 24 * 60 * 60 * 1000L // a fast can be at most 7 days


/**
 * One fast. While [active] is true, [endTime] is the planned end (the goal). When the fast is ended, or was
 * logged after the fact, [active] is false and [endTime] is when it actually finished.
 *
 * This data class represents a fasting record returned by the backend.
 * It contains information such as who the fast belongs to, when it started,
 * when it ended, and whether it is currently active.
 */
@Serializable
data class FastResponse(

    // Unique identifier for this fasting record.
    val fastId: String,

    // Identifies the user who created this fasting record.
    val userId: String,

    // The time when the fast started, stored as epoch milliseconds.
    val startTime: Long,     // epoch millis

    // The planned or actual time when the fast ends, stored as epoch milliseconds.
    val endTime: Long,       // epoch millis

    // Indicates whether the fast is currently active.
    val active: Boolean,

    // Records when the fasting entry was originally created.
    val createdAt: Long,

    // Records when the fasting entry was last updated.
    val updatedAt: Long
)


// This data class represents the information sent to the backend
// when the user creates a new fasting entry.
@Serializable
data class CreateFastRequest(

    // The time at which the user wants the fast to begin.
    val startTime: Long,

    // The planned time at which the fast should finish.
    val endTime: Long
) {

    // Determines whether the fast should currently be considered active.
    // If the end time is still in the future, the fast has not finished yet.
    fun isActive(): Boolean = endTime > System.currentTimeMillis()

    // Validates the start and end times before the fasting entry is created.
    // The validation function returns a list of any errors that were found.
    fun validate(): List<String> = validateFastTimes(startTime, endTime, isActive())
}


// This data class represents the information that can be changed
// when updating an existing fasting record.
@Serializable
data class UpdateFastRequest(

    // The new start time. It is nullable because the user does not
    // necessarily need to change the start time when updating a fast.
    val startTime: Long? = null,

    // The new end time. It is also nullable because updating a fast
    // does not always require changing its end time.
    val endTime: Long? = null,

    // Allows the active status of the fast to be updated.
    val active: Boolean? = null
)


// This function performs the validation rules that apply to fasting times.
// It is separate from the request classes so the same validation logic
// can be reused wherever fasting times need to be checked.
fun validateFastTimes(startTime: Long, endTime: Long, active: Boolean): List<String> {

    // Stores all validation errors so that multiple problems
    // can be reported at the same time.
    val errors = mutableListOf<String>()

    // Gets the current time from the device/server in epoch milliseconds.
    // This is used to check whether the supplied timestamps are valid.
    val now = System.currentTimeMillis()

    // Both timestamps must be greater than zero to be considered valid.
    if (startTime <= 0 || endTime <= 0) errors.add("Start and end must be valid timestamps")

    // The end time must occur after the start time.
    // This prevents a fast from having a negative or zero duration.
    if (endTime <= startTime) errors.add("The end must be after the start")

    // Prevents a user from creating a fast that lasts longer than 7 days.
    if (endTime - startTime > MAX_FAST_MS) errors.add("A fast can be at most 7 days long")

    // Prevents the start time from being too far into the future.
    // MAX_SKEW_MS allows a small difference between the device and server clocks.
    if (startTime > now + MAX_SKEW_MS) errors.add("The start can't be in the future")

    // If the fast is marked as finished, its end time cannot be
    // significantly in the future.
    if (!active && endTime > now + MAX_SKEW_MS) errors.add("A finished fast can't end in the future")

    // Returns all errors found.
    // An empty list means that the fasting times passed all validation checks.
    return errors
}
// ------------------ END OF FILE ------------------ //