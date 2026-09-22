package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMedicationRequest
import com.example.kenkosahealthapp_prog7314.data.network.MedicationApi
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SetTakenRequest
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMedicationRequest
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for managing medication and vitamin records.
// It communicates with the backend through MedicationApi.
class MedicationRepository(
    // Uses the shared Retrofit MedicationApi instance by default.
    private val api: MedicationApi = RetrofitInstance.medicationApi
) {

    // Retrieves all medications belonging to the authenticated user.
    suspend fun getMedications(): ApiResult<List<MedicationDto>> =
        safeCall {
            api.getMedications()
        }

    // Retrieves a specific medication using its unique medication ID.
    suspend fun getMedication(
        medicationId: String
    ): ApiResult<MedicationDto> =
        safeCall {
            api.getMedication(medicationId)
        }

    // Creates a new medication or vitamin record.
    suspend fun createMedication(
        // Category of the item, such as MEDICATION or VITAMIN.
        category: String,

        // Name of the medication or vitamin.
        name: String,

        // Dosage information.
        dosage: String,

        // Frequency, such as DAILY, WEEKLY or MONTHLY.
        frequency: String,

        // Determines whether reminders are enabled.
        remindersEnabled: Boolean,

        // Time at which the reminder should occur.
        reminderTime: String,

        // Date on which the medication starts.
        startDate: String
    ): ApiResult<MedicationDto> {

        // Validates the medication name and dosage before
        // sending the request to the backend.
        validate(name, dosage)?.let {
            return ApiResult.ValidationError(it)
        }

        return safeCall {

            // Creates the request object and sends it to the backend.
            api.createMedication(
                CreateMedicationRequest(
                    category = category,

                    // Removes unnecessary whitespace from the medication name.
                    name = name.trim(),

                    // Removes unnecessary whitespace from the dosage.
                    dosage = dosage.trim(),

                    frequency = frequency,
                    remindersEnabled = remindersEnabled,
                    reminderTime = reminderTime,
                    startDate = startDate
                )
            )
        }
    }

    // Updates an existing medication or vitamin record.
    suspend fun updateMedication(
        // ID of the medication being updated.
        medicationId: String,

        // Updated medication category.
        category: String,

        // Updated medication name.
        name: String,

        // Updated dosage.
        dosage: String,

        // Updated medication frequency.
        frequency: String,

        // Updated reminder setting.
        remindersEnabled: Boolean,

        // Updated reminder time.
        reminderTime: String,

        // Updated starting date.
        startDate: String?
    ): ApiResult<MedicationDto> {

        // Validates the medication name and dosage before
        // sending the update to the backend.
        validate(name, dosage)?.let {
            return ApiResult.ValidationError(it)
        }

        return safeCall {

            // Creates the update request and sends it to the backend.
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

    // Updates whether a medication was taken on a specific date.
    suspend fun setTaken(
        medicationId: String,
        date: String,
        taken: Boolean
    ): ApiResult<MedicationDto> =
        safeCall {
            api.setTaken(
                medicationId,
                SetTakenRequest(date, taken)
            )
        }

    // Deletes a medication or vitamin record using its unique ID.
    suspend fun deleteMedication(
        medicationId: String
    ): ApiResult<Unit> =
        safeCall {
            api.deleteMedication(medicationId)
        }

    // Validates the medication name and dosage.
    // Returns an error message when validation fails,
    // or null when the supplied values are valid.
    private fun validate(
        name: String,
        dosage: String
    ): String? = when {

        // Medication name cannot be empty.
        name.isBlank() ->
            "Medication name cannot be empty"

        // Medication name cannot exceed 60 characters.
        name.trim().length > 60 ->
            "Medication name must be 60 characters or fewer"

        // Dosage cannot be empty.
        dosage.isBlank() ->
            "Dosage cannot be empty"

        // Dosage cannot exceed 30 characters.
        dosage.trim().length > 30 ->
            "Dosage must be 30 characters or fewer"

        // No validation errors were found.
        else -> null
    }

    // Executes an API operation safely and converts exceptions
    // into the appropriate ApiResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): ApiResult<T> = try {

        // API request completed successfully.
        ApiResult.Success(block())

    } catch (e: HttpException) {

        // Handles HTTP errors returned by the backend.
        val message = e.response()?.errorBody()?.string()
            ?: e.message()

        // Returns the HTTP status code and server error message.
        ApiResult.ServerError(
            e.code(),
            message ?: "Server error"
        )

    } catch (e: IOException) {

        // Handles network connectivity problems.
        ApiResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles unexpected errors.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //