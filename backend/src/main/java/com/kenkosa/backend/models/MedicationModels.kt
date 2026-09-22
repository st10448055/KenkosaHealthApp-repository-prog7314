package com.kenkosa.backend.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

// Represents whether the item is a medication or vitamin.
@Serializable
enum class MedicationCategory { MEDICATION, VITAMIN }

// Defines how frequently the medication should be taken.
@Serializable
enum class MedicationFrequency { DAILY, WEEKLY, MONTHLY }

// Used to make sure reminder times follow the 24-hour HH:mm format.
private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

// Checks whether a date is in the correct yyyy-MM-dd format.
private fun String.isIsoDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess

// Represents a medication record returned from the backend.
@Serializable
data class MedicationResponse(
    val medicationId: String,
    val userId: String,
    val category: MedicationCategory,
    val name: String,
    val dosage: String,                 // free text, e.g. "500mg"
    val frequency: MedicationFrequency,
    val remindersEnabled: Boolean,
    val reminderTime: String,           // "HH:mm" (24h)
    val startDate: String,              // "yyyy-MM-dd" - anchor for weekly/monthly schedules
    val takenDates: List<String>,       // "yyyy-MM-dd" dates the dose was marked as taken
    val createdAt: Long,
    val updatedAt: Long
)

// Represents the information needed when creating a new medication.
@Serializable
data class CreateMedicationRequest(
    val category: MedicationCategory = MedicationCategory.MEDICATION,
    val name: String,
    val dosage: String,
    val frequency: MedicationFrequency,
    val remindersEnabled: Boolean = false,
    val reminderTime: String = "08:00",
    val startDate: String
) {

    // Validates the medication details before they are saved.
    // All validation errors are collected and returned together.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // A medication must have a name within the allowed length.
        if (name.isBlank()) errors.add("Medication name cannot be empty")
        if (name.length > 60) errors.add("Medication name must be 60 characters or fewer")

        // Dosage is required and has a maximum length.
        if (dosage.isBlank()) errors.add("Dosage cannot be empty")
        if (dosage.length > 30) errors.add("Dosage must be 30 characters or fewer")

        // Makes sure the reminder time uses the required HH:mm format.
        if (!TIME_REGEX.matches(reminderTime)) errors.add("Reminder time must be in HH:mm format")

        // Makes sure the starting date is a valid yyyy-MM-dd date.
        if (!startDate.isIsoDate()) errors.add("Start date must be in yyyy-MM-dd format")

        return errors
    }
}

// Represents the fields that can be changed when updating a medication.
// The nullable fields allow the user to update only specific information.
@Serializable
data class UpdateMedicationRequest(
    val category: MedicationCategory? = null,
    val name: String? = null,
    val dosage: String? = null,
    val frequency: MedicationFrequency? = null,
    val remindersEnabled: Boolean? = null,
    val reminderTime: String? = null,
    val startDate: String? = null
) {

    // Validates only the values that were provided in the update request.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // The let block only performs validation when a new name was supplied.
        name?.let {
            if (it.isBlank()) errors.add("Medication name cannot be empty")
            if (it.length > 60) errors.add("Medication name must be 60 characters or fewer")
        }

        // Checks the dosage only when a new dosage was provided.
        dosage?.let {
            if (it.isBlank()) errors.add("Dosage cannot be empty")
            if (it.length > 30) errors.add("Dosage must be 30 characters or fewer")
        }

        // Checks that an updated reminder time has the correct format.
        reminderTime?.let { if (!TIME_REGEX.matches(it)) errors.add("Reminder time must be in HH:mm format") }

        // Checks that an updated start date has the correct format.
        startDate?.let { if (!it.isIsoDate()) errors.add("Start date must be in yyyy-MM-dd format") }

        return errors
    }
}

// Represents a request to mark a medication as either taken or not taken
// on a specific date.
@Serializable
data class SetTakenRequest(
    val date: String,   // "yyyy-MM-dd", the client's local date
    val taken: Boolean
) {

    // Validates the date before recording the medication status.
    // Returns no errors when the date is valid.
    fun validate(): List<String> =
        if (date.isIsoDate()) emptyList() else listOf("Date must be in yyyy-MM-dd format")
}


// ------------------ END OF FILE ------------------ //