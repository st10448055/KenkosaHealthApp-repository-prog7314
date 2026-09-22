package com.example.kenkosahealthapp_prog7314.data.network

 // Data Transfer Object (DTO) representing a mood entry returned by the backend.
 // This contains the mood selected by the user, associated feelings,
 // and timestamps for the mood record.
data class MoodEntryDto(

    // Unique identifier for the mood entry.
    val moodId: String,

    // Firebase user ID belonging to the mood entry.
    val userId: String,

    // Main mood selected by the user.
    // Expected values:
    // VERY_HAPPY | HAPPY | NEUTRAL | SAD | VERY_SAD
    val mood: String,

    // Additional feelings selected by the user.
    // Examples include CALM, CONTENT, EXCITED, etc.
    val feelings: List<String>,

    // Time when the mood was logged, stored as epoch milliseconds.
    val loggedAt: Long,

    // Timestamp indicating when the mood entry was created.
    val createdAt: Long,

    // Timestamp indicating when the mood entry was last updated.
    val updatedAt: Long
)


 // Request object used when creating a new mood entry.
 // The mood ID, user ID and creation timestamps are handled by the backend.
data class CreateMoodRequest(

    // Main mood selected by the user.
    val mood: String,

    // List of additional feelings selected by the user.
    val feelings: List<String>,

    // Time when the mood was logged, stored as epoch milliseconds.
    val loggedAt: Long
)


 // Request object used when updating an existing mood entry.
 // All properties are nullable so that the application can send
 // only the values that need to be changed.
data class UpdateMoodRequest(

    // Updated main mood.
    // Null means the existing mood should remain unchanged.
    val mood: String? = null,

    // Updated list of feelings.
    // Null means the existing feelings should remain unchanged.
    val feelings: List<String>? = null,

    // Updated logging time.
    // Null means the existing logged time should remain unchanged.
    val loggedAt: Long? = null
)

// ------------------ END OF FILE ------------------ //