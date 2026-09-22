package com.example.kenkosahealthapp_prog7314.data.network

/** One night's sleep. Times are epoch millis. */
data class SleepLogDto(
    val sleepId: String,
    val userId: String,
    val sleepStart: Long,
    val sleepEnd: Long,
    val totalDurationMinutes: Int,
    val qualityRating: Int,        // 1..5 (Poor..Excellent)
    val qualityScore: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long?
)

/** Used to log a sleep and to edit one. */
data class SaveSleepLogRequest(
    val sleepStart: Long,
    val sleepEnd: Long,
    val qualityRating: Int,
    val notes: String?
)
