package com.example.kenkosahealthapp_prog7314.data.network


 // Data Transfer Object (DTO) representing one night's sleep.
 // This object is returned by the backend API and contains
// the stored sleep information for the authenticated user.
 // Sleep times are stored as epoch milliseconds.

data class SleepLogDto(
    // Unique identifier for the sleep record.
    val sleepId: String,

    // Firebase user ID belonging to the sleep record.
    val userId: String,

    // Time when the user went to sleep, stored as epoch milliseconds.
    val sleepStart: Long,

    // Time when the user woke up, stored as epoch milliseconds.
    val sleepEnd: Long,

    // Total duration of the sleep period, measured in minutes.
    val totalDurationMinutes: Int,

    // User's sleep quality rating from 1 to 5.
    // 1 = Poor and 5 = Excellent.
    val qualityRating: Int,

    // Calculated sleep quality score returned by the backend.
    val qualityScore: Int,

    // Optional notes entered by the user about their sleep.
    // Nullable because the user does not have to provide notes.
    val notes: String?,

    // Timestamp indicating when the sleep record was created.
    val createdAt: Long,

    // Timestamp indicating when the sleep record was last updated.
    // Nullable because an existing record may not have an update timestamp.
    val updatedAt: Long?
)


 // Request object used when creating or editing a sleep record.
 // This contains only the information that the Android application
 // needs to send to the backend. IDs and timestamps are handled
 // by the backend.

data class SaveSleepLogRequest(
    // Time when the sleep period started, stored as epoch milliseconds.
    val sleepStart: Long,

    // Time when the sleep period ended, stored as epoch milliseconds.
    val sleepEnd: Long,

    // User-provided sleep quality rating from 1 to 5.
    val qualityRating: Int,

    // Optional notes about the sleep session.
    val notes: String?
)

// ------------------ END OF FILE ------------------ //