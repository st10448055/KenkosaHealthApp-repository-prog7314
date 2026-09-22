package com.example.kenkosahealthapp_prog7314.data.network


 // Data Transfer Object (DTO) representing the user's water intake
 // for a specific day.

data class WaterIntakeDto(

    // Date that the water intake record belongs to.
    val date: String,

    // Number of cups of water the user has consumed.
    val cupsConsumed: Int,

    // Target number of cups the user should consume for the day.
    val targetCups: Int
)


// Request object used when updating the user's daily water intake.
data class UpdateWaterRequest(

    // New number of cups consumed by the user.
    // This value is sent to the backend when updating water intake.
    val cupsConsumed: Int
)

// ------------------ END OF FILE ------------------ //