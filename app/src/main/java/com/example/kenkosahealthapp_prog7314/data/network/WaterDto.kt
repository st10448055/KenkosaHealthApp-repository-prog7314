package com.example.kenkosahealthapp_prog7314.data.network

data class WaterIntakeDto(
    val date: String,
    val cupsConsumed: Int,
    val targetCups: Int
)

data class UpdateWaterRequest(
    val cupsConsumed: Int
)