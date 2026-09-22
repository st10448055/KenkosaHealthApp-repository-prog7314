package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

// Defines the Retrofit API endpoints used to manage fasting records.
// Retrofit uses the annotations below to convert these functions
// into HTTP requests to the backend API.
interface FastingApi {

    // Sends a GET request to /api/fasts.
    // Retrieves all fasting records belonging to the authenticated user.
    suspend fun getFasts(): List<FastDto>

    // Sends a GET request to /api/fasts/{id}.
    // Retrieves a specific fasting record using its ID.
    @GET("api/fasts/{id}")
    suspend fun getFast(
        @Path("id") fastId: String
    ): FastDto

    // Sends a POST request to /api/fasts.
    // Creates a new fasting record using the supplied request data.
    @POST("api/fasts")
    suspend fun createFast(
        @Body request: CreateFastRequest
    ): FastDto

    // Sends a PATCH request to /api/fasts/{id}.
    // Updates an existing fasting record using its ID and
    // the fields supplied in the update request.
    @PATCH("api/fasts/{id}")
    suspend fun updateFast(
        @Path("id") fastId: String,
        @Body request: UpdateFastRequest
    ): FastDto

    // Sends a DELETE request to /api/fasts/{id}.
    // Removes the specified fasting record from the backend.
    @DELETE("api/fasts/{id}")
    suspend fun deleteFast(
        @Path("id") fastId: String
    )
}

// ------------------ END OF FILE ------------------ //