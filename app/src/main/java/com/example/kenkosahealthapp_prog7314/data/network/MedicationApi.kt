package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

// Defines the Retrofit API endpoints used to manage
// the user's medications and vitamins.
interface MedicationApi {

    // Sends a GET request to /api/medications.
    // Retrieves all medications and vitamins belonging
    // to the authenticated user.
    @GET("api/medications")
    suspend fun getMedications(): List<MedicationDto>

    // Sends a GET request to /api/medications/{id}.
    // Retrieves a specific medication using its ID.
    @GET("api/medications/{id}")
    suspend fun getMedication(
        @Path("id") medicationId: String
    ): MedicationDto

    // Sends a POST request to /api/medications.
    // Creates a new medication or vitamin record.
    @POST("api/medications")
    suspend fun createMedication(
        @Body request: CreateMedicationRequest
    ): MedicationDto

    // Sends a PATCH request to /api/medications/{id}.
    // Updates an existing medication using its ID.
    //
    // Only the fields provided in UpdateMedicationRequest
    // need to be changed.
    @PATCH("api/medications/{id}")
    suspend fun updateMedication(
        @Path("id") medicationId: String,
        @Body request: UpdateMedicationRequest
    ): MedicationDto

    // Sends a PUT request to /api/medications/{id}/taken.
    // Marks a medication as taken or not taken for a
    // specific date.
    @PUT("api/medications/{id}/taken")
    suspend fun setTaken(
        @Path("id") medicationId: String,
        @Body request: SetTakenRequest
    ): MedicationDto

    // Sends a DELETE request to /api/medications/{id}.
    // Deletes the specified medication or vitamin record.
    @DELETE("api/medications/{id}")
    suspend fun deleteMedication(
        @Path("id") medicationId: String
    )
}

// ------------------ END OF FILE ------------------ //