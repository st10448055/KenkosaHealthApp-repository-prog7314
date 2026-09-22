package com.example.kenkosahealthapp_prog7314.data.network

// Data Transfer Object (DTO) representing a fasting record
// returned by the backend API.
data class FastDto(

    // Unique identifier for the fasting record.
    val fastId: String,

    // Unique identifier of the user who owns the fasting record.
    val userId: String,

    // Timestamp indicating when the fast started.
    val startTime: Long,

    // Timestamp indicating when the fast ended.
    val endTime: Long,

    // Indicates whether the fasting session is currently active.
    val active: Boolean,

    // Timestamp indicating when the fasting record was created.
    val createdAt: Long,

    // Timestamp indicating when the fasting record was last updated.
    val updatedAt: Long
)

// Request object used when creating a new fasting record.
data class CreateFastRequest(

    // Timestamp indicating when the fasting session starts.
    val startTime: Long,

    // Timestamp indicating when the fasting session should end.
    val endTime: Long
)

// Request object used when updating an existing fasting record.
// All properties are nullable so that only the values that need
// to be changed have to be included in the request.
data class UpdateFastRequest(

    // New start timestamp for the fasting session, if provided.
    val startTime: Long? = null,

    // New end timestamp for the fasting session, if provided.
    val endTime: Long? = null,

    // Updated active status of the fasting session, if provided.
    // true means the fast is active, while false means it is not active.
    val active: Boolean? = null
)

// ------------------ END OF FILE ------------------ //