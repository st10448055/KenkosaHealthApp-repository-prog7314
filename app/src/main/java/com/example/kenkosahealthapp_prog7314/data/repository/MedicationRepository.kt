package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMedicationRequest
import com.example.kenkosahealthapp_prog7314.data.network.MedicationApi
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SetTakenRequest
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMedicationRequest
import retrofit2.HttpException
import java.io.IOException

class MedicationRepository(private val api: MedicationApi = RetrofitInstance.medicationApi) {

    suspend fun getMedications(): ApiResult<List<MedicationDto>> = safeCall { api.getMedications() }

    suspend fun getMedication(medicationId: String): ApiResult<MedicationDto> =
        safeCall { api.getMedication(medicationId) }

    suspend fun createMedication(
        category: String,
        name: String,
        dosage: String,
        frequency: String,
        remindersEnabled: Boolean,
        reminderTime: String,
        startDate: String
    ): ApiResult<MedicationDto> {
        validate(name, dosage)?.let { return ApiResult.ValidationError(it) }

        return safeCall {
            api.createMedication(
                CreateMedicationRequest(
                    category = category,
                    name = name.trim(),
                    dosage = dosage.trim(),
                    frequency = frequency,
                    remindersEnabled = remindersEnabled,
                    reminderTime = reminderTime,
                    startDate = startDate
                )
            )
        }
    }

    suspend fun updateMedication(
        medicationId: String,
        category: String,
        name: String,
        dosage: String,
        frequency: String,
        remindersEnabled: Boolean,
        reminderTime: String,
        startDate: String?
    ): ApiResult<MedicationDto> {
        validate(name, dosage)?.let { return ApiResult.ValidationError(it) }

        return safeCall {
            api.updateMedication(
                medicationId,
                UpdateMedicationRequest(
                    category = category,
                    name = name.trim(),
                    dosage = dosage.trim(),
                    frequency = frequency,
                    remindersEnabled = remindersEnabled,
                    reminderTime = reminderTime,
                    startDate = startDate
                )
            )
        }
    }

    suspend fun setTaken(medicationId: String, date: String, taken: Boolean): ApiResult<MedicationDto> =
        safeCall { api.setTaken(medicationId, SetTakenRequest(date, taken)) }

    suspend fun deleteMedication(medicationId: String): ApiResult<Unit> =
        safeCall { api.deleteMedication(medicationId) }

    private fun validate(name: String, dosage: String): String? = when {
        name.isBlank() -> "Medication name cannot be empty"
        name.trim().length > 60 -> "Medication name must be 60 characters or fewer"
        dosage.isBlank() -> "Dosage cannot be empty"
        dosage.trim().length > 30 -> "Dosage must be 30 characters or fewer"
        else -> null
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string() ?: e.message()
        ApiResult.ServerError(e.code(), message ?: "Server error")
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }
}