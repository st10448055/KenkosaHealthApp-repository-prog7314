package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// Represents the different levels a user can select for their mood.
@Serializable
enum class MoodLevel { VERY_HAPPY, HAPPY, NEUTRAL, SAD, VERY_SAD }

// Represents additional feelings that can be selected alongside the mood.
@Serializable
enum class MoodFeeling { CALM, CONTENT, EXCITED, UPSET, IRRITATED, HAPPY, MOTIVATED, GRATEFUL, PROUD }

/** Allow a little clock drift between the phone and the server. */
// Allows a small difference between the phone's time and the server's time.
private const val MAX_FUTURE_SKEW_MS = 5 * 60 * 1000L

// Represents a mood entry returned by the backend.
@Serializable
data class MoodEntryResponse(
    val moodId: String,
    val userId: String,
    val mood: MoodLevel,
    val feelings: List<MoodFeeling>,
    val loggedAt: Long,          // epoch millis of the moment the user says they felt this way
    val createdAt: Long,
    val updatedAt: Long
)

// Represents the information sent when creating a new mood entry.
@Serializable
data class CreateMoodRequest(
    val mood: MoodLevel,
    val feelings: List<MoodFeeling> = emptyList(),
    val loggedAt: Long
) {

    // Validates the mood entry before it is saved.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Makes sure the timestamp is valid and not set too far into the future.
        if (loggedAt <= 0) errors.add("loggedAt must be a valid timestamp")
        if (loggedAt > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) errors.add("Mood cannot be logged in the future")

        return errors
    }
}

// Represents the information that can be changed when updating
// an existing mood entry. Nullable fields allow partial updates.
@Serializable
data class UpdateMoodRequest(
    val mood: MoodLevel? = null,
    val feelings: List<MoodFeeling>? = null,
    val loggedAt: Long? = null
) {

    // Validates the values provided in the update request.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Only validates the timestamp when a new one was provided.
        loggedAt?.let {
            if (it <= 0) errors.add("loggedAt must be a valid timestamp")
            if (it > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) errors.add("Mood cannot be logged in the future")
        }

        return errors
    }
}


// ------------------ END OF FILE ------------------ //