package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class WaterIntakeResponse(
    val date: String,        // "yyyy-MM-dd"
    val cupsConsumed: Int,
    val targetCups: Int
)

@Serializable
data class UpdateWaterRequest(
    val cupsConsumed: Int
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (cupsConsumed < 0) errors.add("Cups consumed cannot be negative")
        if (cupsConsumed > 20) errors.add("That seems like an unusually high number of cups")
        return errors
    }
}