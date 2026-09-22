package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// Represents the user's water intake for a specific day.
@Serializable
data class WaterIntakeResponse(
    val date: String,        // "yyyy-MM-dd"

    // Number of cups the user has consumed.
    val cupsConsumed: Int,

    // The user's target number of cups for the day.
    val targetCups: Int
)

// Represents the information sent when updating the user's water intake.
@Serializable
data class UpdateWaterRequest(
    val cupsConsumed: Int
) {

    // Validates the number of cups before the intake is updated.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Water intake cannot be a negative number.
        if (cupsConsumed < 0) errors.add("Cups consumed cannot be negative")

        // Prevents an unusually high number of cups from being recorded.
        if (cupsConsumed > 20) errors.add("That seems like an unusually high number of cups")

        return errors
    }
}


// ------------------ END OF FILE ------------------ //