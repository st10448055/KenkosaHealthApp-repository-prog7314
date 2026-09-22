package com.example.kenkosahealthapp_prog7314.data.network


data class FastDto(
    val fastId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class CreateFastRequest(
    val startTime: Long,
    val endTime: Long
)

data class UpdateFastRequest(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val active: Boolean? = null
)