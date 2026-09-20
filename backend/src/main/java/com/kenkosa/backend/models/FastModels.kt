package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

/** Allow a little clock drift between the phone and the server. */
private const val MAX_SKEW_MS = 5 * 60 * 1000L
private const val MAX_FAST_MS = 7 * 24 * 60 * 60 * 1000L // a fast can be at most 7 days

/**
 * One fast. While [active] is true, [endTime] is the planned end (the goal). When the fast is ended, or was
 * logged after the fact, [active] is false and [endTime] is when it actually finished.
 */
@Serializable
data class FastResponse(
    val fastId: String,
    val userId: String,
    val startTime: Long,     // epoch millis
    val endTime: Long,       // epoch millis
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateFastRequest(
    val startTime: Long,
    val endTime: Long
) {
    /** A fast whose end is still in the future is the user's current (active) fast. */
    fun isActive(): Boolean = endTime > System.currentTimeMillis()

    fun validate(): List<String> = validateFastTimes(startTime, endTime, isActive())
}

@Serializable
data class UpdateFastRequest(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val active: Boolean? = null
)

fun validateFastTimes(startTime: Long, endTime: Long, active: Boolean): List<String> {
    val errors = mutableListOf<String>()
    val now = System.currentTimeMillis()

    if (startTime <= 0 || endTime <= 0) errors.add("Start and end must be valid timestamps")
    if (endTime <= startTime) errors.add("The end must be after the start")
    if (endTime - startTime > MAX_FAST_MS) errors.add("A fast can be at most 7 days long")
    if (startTime > now + MAX_SKEW_MS) errors.add("The start can't be in the future")
    if (!active && endTime > now + MAX_SKEW_MS) errors.add("A finished fast can't end in the future")
    return errors
}