package com.example.kenkosahealthapp_prog7314.data.network

// Data Transfer Object (DTO) representing a medication or vitamin
// record returned by the backend API.
data class MedicationDto(

    // Unique identifier for the medication record.
    val medicationId: String,

    // Unique identifier of the user who owns the medication.
    val userId: String,

    // Category of the item.
    // Possible values are "MEDICATION" or "VITAMIN".
    val category: String,

    // Name of the medication or vitamin.
    val name: String,

    // Dosage information entered as free text.
    // Example: "500mg".
    val dosage: String,

    // Frequency at which the medication should be taken.
    // Possible values are "DAILY", "WEEKLY", or "MONTHLY".
    val frequency: String,

    // Indicates whether reminders are enabled for this medication.
    val remindersEnabled: Boolean,

    // Time at which the reminder should be displayed.
    // Stored using the 24-hour "HH:mm" format.
    val reminderTime: String,

    // Date on which the medication schedule starts.
    // Stored using the "yyyy-MM-dd" format.
    // This date is also used as the starting point for
    // calculating weekly and monthly schedules.
    val startDate: String,

    // List of dates on which the user marked the medication
    // as taken.
    // Dates use the "yyyy-MM-dd" format.
    val takenDates: List<String>,

    // Timestamp indicating when the medication record was created.
    val createdAt: Long,

    // Timestamp indicating when the medication record
    // was most recently updated.
    val updatedAt: Long
)

// Request object used when creating a new medication or vitamin.
data class CreateMedicationRequest(

    // Category of the item being created.
    val category: String,

    // Name of the medication or vitamin.
    val name: String,

    // Dosage information, such as "500mg".
    val dosage: String,

    // Frequency of the medication schedule.
    val frequency: String,

    // Determines whether medication reminders are enabled.
    val remindersEnabled: Boolean,

    // Time at which the reminder should be triggered.
    // Uses the "HH:mm" 24-hour format.
    val reminderTime: String,

    // Starting date for the medication schedule.
    // Uses the "yyyy-MM-dd" format.
    val startDate: String
)

// Request object used when updating an existing medication.
// All properties are nullable so that only the fields that
// need to be changed have to be included in the request.
data class UpdateMedicationRequest(

    // Updated medication category, if provided.
    val category: String? = null,

    // Updated medication or vitamin name, if provided.
    val name: String? = null,

    // Updated dosage, if provided.
    val dosage: String? = null,

    // Updated medication frequency, if provided.
    val frequency: String? = null,

    // Updated reminder setting, if provided.
    val remindersEnabled: Boolean? = null,

    // Updated reminder time, if provided.
    val reminderTime: String? = null,

    // Updated schedule start date, if provided.
    val startDate: String? = null
)

// Request object used to mark a medication as taken
// or not taken for a specific date.
data class SetTakenRequest(

    // Date for which the medication status is being recorded.
    // Uses the phone's local date in "yyyy-MM-dd" format.
    val date: String,

    // Indicates whether the medication was taken on the specified date.
    // true = taken, false = not taken.
    val taken: Boolean
)

// ------------------ END OF FILE ------------------ //