package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class SleepRecordResponse(
    val sleepId: String,
    val userId: String,
    val sleepStart: Long,          // epoch millis
    val sleepEnd: Long,            // epoch millis
    val totalDurationMinutes: Int,
    val qualityRating: Int,        // 1..5 (Poor..Excellent)
    val qualityScore: Int,         // derived: rating * 20
    val notes: String?,
    val createdAt: Long
)

@Serializable
data class CreateSleepRequest(
    val sleepStart: Long,
    val sleepEnd: Long,
    val qualityRating: Int,
    val notes: String? = null
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (sleepStart <= 0) errors.add("Sleep start time is required")
        if (sleepEnd <= 0) errors.add("Sleep end time is required")
        if (qualityRating !in 1..5) errors.add("Quality rating must be between 1 and 5")
        notes?.let { if (it.length > 500) errors.add("Notes must be 500 characters or fewer") }
        return errors
    }
}