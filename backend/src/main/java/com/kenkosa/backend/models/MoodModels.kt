package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
enum class MoodLevel { VERY_HAPPY, HAPPY, NEUTRAL, SAD, VERY_SAD }

@Serializable
enum class MoodFeeling { CALM, CONTENT, EXCITED, UPSET, IRRITATED, HAPPY, MOTIVATED, GRATEFUL, PROUD }

/** Allow a little clock drift between the phone and the server. */
private const val MAX_FUTURE_SKEW_MS = 5 * 60 * 1000L

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

@Serializable
data class CreateMoodRequest(
    val mood: MoodLevel,
    val feelings: List<MoodFeeling> = emptyList(),
    val loggedAt: Long
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (loggedAt <= 0) errors.add("loggedAt must be a valid timestamp")
        if (loggedAt > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) errors.add("Mood cannot be logged in the future")
        return errors
    }
}

@Serializable
data class UpdateMoodRequest(
    val mood: MoodLevel? = null,
    val feelings: List<MoodFeeling>? = null,
    val loggedAt: Long? = null
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        loggedAt?.let {
            if (it <= 0) errors.add("loggedAt must be a valid timestamp")
            if (it > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) errors.add("Mood cannot be logged in the future")
        }
        return errors
    }
}
