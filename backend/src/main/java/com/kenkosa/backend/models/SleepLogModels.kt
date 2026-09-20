package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

/** Allow a little clock drift between the phone and the server. */
private const val SLEEP_MAX_SKEW_MS = 5 * 60 * 1000L
private const val SLEEP_MAX_DURATION_MS = 24L * 60 * 60 * 1000

/** Own error type, so this feature doesn't depend on (or clash with) any other model file. */
@Serializable
data class SleepLogErrorResponse(val errors: List<String>)

/**
 * One night's sleep. Same Firestore documents ("sleepRecords") as the original sleep endpoint, so anything already
 * logged still shows up. [qualityScore] is kept as rating x 20 for compatibility; the app works out its own
 * 0-100 sleep score from the rating and the duration.
 */
@Serializable
data class SleepLogResponse(
    val sleepId: String,
    val userId: String,
    val sleepStart: Long,          // epoch millis
    val sleepEnd: Long,            // epoch millis
    val totalDurationMinutes: Int,
    val qualityRating: Int,        // 1..5 (Poor..Excellent)
    val qualityScore: Int,         // rating * 20
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/** Used to log a sleep and to edit one (every field is sent). */
@Serializable
data class SaveSleepLogRequest(
    val sleepStart: Long,
    val sleepEnd: Long,
    val qualityRating: Int,
    val notes: String? = null
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val now = System.currentTimeMillis()

        if (sleepStart <= 0 || sleepEnd <= 0) errors.add("Bedtime and wake-up time are required")
        if (sleepEnd <= sleepStart) errors.add("Wake-up time must be after bedtime")
        if (sleepEnd - sleepStart > SLEEP_MAX_DURATION_MS) errors.add("A sleep can be at most 24 hours long")
        if (sleepEnd > now + SLEEP_MAX_SKEW_MS) errors.add("Wake-up time can't be in the future")
        if (qualityRating !in 1..5) errors.add("Quality rating must be between 1 and 5")
        notes?.let { if (it.length > 500) errors.add("Notes must be 500 characters or fewer") }
        return errors
    }
}
