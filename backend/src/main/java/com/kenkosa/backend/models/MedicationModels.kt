package com.kenkosa.backend.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class MedicationCategory { MEDICATION, VITAMIN }

@Serializable
enum class MedicationFrequency { DAILY, WEEKLY, MONTHLY }

private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun String.isIsoDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess

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
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Medication name cannot be empty")
        if (name.length > 60) errors.add("Medication name must be 60 characters or fewer")
        if (dosage.isBlank()) errors.add("Dosage cannot be empty")
        if (dosage.length > 30) errors.add("Dosage must be 30 characters or fewer")
        if (!TIME_REGEX.matches(reminderTime)) errors.add("Reminder time must be in HH:mm format")
        if (!startDate.isIsoDate()) errors.add("Start date must be in yyyy-MM-dd format")
        return errors
    }
}

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
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        name?.let {
            if (it.isBlank()) errors.add("Medication name cannot be empty")
            if (it.length > 60) errors.add("Medication name must be 60 characters or fewer")
        }
        dosage?.let {
            if (it.isBlank()) errors.add("Dosage cannot be empty")
            if (it.length > 30) errors.add("Dosage must be 30 characters or fewer")
        }
        reminderTime?.let { if (!TIME_REGEX.matches(it)) errors.add("Reminder time must be in HH:mm format") }
        startDate?.let { if (!it.isIsoDate()) errors.add("Start date must be in yyyy-MM-dd format") }
        return errors
    }
}

@Serializable
data class SetTakenRequest(
    val date: String,   // "yyyy-MM-dd", the client's local date
    val taken: Boolean
) {
    fun validate(): List<String> =
        if (date.isIsoDate()) emptyList() else listOf("Date must be in yyyy-MM-dd format")
}
