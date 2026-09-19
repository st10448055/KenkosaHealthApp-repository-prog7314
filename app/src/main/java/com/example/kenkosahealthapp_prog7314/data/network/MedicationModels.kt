package com.example.kenkosahealthapp_prog7314.data.network

data class MedicationDto(
    val medicationId: String,
    val userId: String,
    val category: String,            // "MEDICATION" | "VITAMIN"
    val name: String,
    val dosage: String,              // free text, e.g. "500mg"
    val frequency: String,           // "DAILY" | "WEEKLY" | "MONTHLY"
    val remindersEnabled: Boolean,
    val reminderTime: String,        // "HH:mm" (24h)
    val startDate: String,           // "yyyy-MM-dd", anchor for weekly / monthly schedules
    val takenDates: List<String>,    // "yyyy-MM-dd" days the dose was marked as taken
    val createdAt: Long,
    val updatedAt: Long
)

data class CreateMedicationRequest(
    val category: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val remindersEnabled: Boolean,
    val reminderTime: String,
    val startDate: String
)

data class UpdateMedicationRequest(
    val category: String? = null,
    val name: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val remindersEnabled: Boolean? = null,
    val reminderTime: String? = null,
    val startDate: String? = null
)

data class SetTakenRequest(
    val date: String,                // "yyyy-MM-dd" (the phone's local date)
    val taken: Boolean
)
