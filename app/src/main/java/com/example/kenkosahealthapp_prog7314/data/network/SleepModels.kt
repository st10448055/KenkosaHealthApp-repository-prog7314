package com.example.kenkosahealthapp_prog7314.data.network

data class SleepRecordDto(
    val sleepId: String,
    val userId: String,
    val sleepStart: Long,
    val sleepEnd: Long,
    val totalDurationMinutes: Int,
    val qualityRating: Int,
    val qualityScore: Int,
    val notes: String?,
    val createdAt: Long
)

data class CreateSleepRequest(
    val sleepStart: Long,
    val sleepEnd: Long,
    val qualityRating: Int,
    val notes: String? = null
)