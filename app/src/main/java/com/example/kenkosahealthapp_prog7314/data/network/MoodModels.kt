package com.example.kenkosahealthapp_prog7314.data.network

data class MoodEntryDto(
    val moodId: String,
    val userId: String,
    val mood: String,               // VERY_HAPPY | HAPPY | NEUTRAL | SAD | VERY_SAD
    val feelings: List<String>,     // CALM, CONTENT, EXCITED, ...
    val loggedAt: Long,             // epoch millis
    val createdAt: Long,
    val updatedAt: Long
)

data class CreateMoodRequest(
    val mood: String,
    val feelings: List<String>,
    val loggedAt: Long
)

data class UpdateMoodRequest(
    val mood: String? = null,
    val feelings: List<String>? = null,
    val loggedAt: Long? = null
)
