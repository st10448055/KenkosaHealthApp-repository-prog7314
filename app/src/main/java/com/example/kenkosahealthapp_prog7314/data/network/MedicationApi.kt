package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.*

interface MedicationApi {
    @GET("api/medications")
    suspend fun getMedications(): List<MedicationDto>

    @GET("api/medications/{id}")
    suspend fun getMedication(@Path("id") medicationId: String): MedicationDto

    @POST("api/medications")
    suspend fun createMedication(@Body request: CreateMedicationRequest): MedicationDto

    @PATCH("api/medications/{id}")
    suspend fun updateMedication(
        @Path("id") medicationId: String,
        @Body request: UpdateMedicationRequest
    ): MedicationDto

    @PUT("api/medications/{id}/taken")
    suspend fun setTaken(
        @Path("id") medicationId: String,
        @Body request: SetTakenRequest
    ): MedicationDto

    @DELETE("api/medications/{id}")
    suspend fun deleteMedication(@Path("id") medicationId: String)
}