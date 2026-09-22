package com.example.kenkosahealthapp_prog7314.data.network

// Data Transfer Object (DTO) representing a tracked health goal
// returned by the backend API.
data class TrackedGoalDto(

    // Unique identifier for the goal.
    val goalId: String,

    // Unique identifier of the user who owns the goal.
    val userId: String,

    // Category of the goal, such as steps, water, sleep, or exercise.
    val category: String,

    // Name given to the goal by the user.
    val name: String,

    // Target value the user wants to achieve.
    val targetValue: Double,

    // Unit used to measure the goal.
    // Examples include steps, litres, minutes, or hours.
    val unit: String,

    // Frequency at which the goal should be completed.
    // For example, daily, weekly, or monthly.
    val frequency: String,

    // Indicates whether reminders are enabled for the goal.
    val remindersEnabled: Boolean,

    // Time at which the goal reminder should be displayed.
    // Nullable because a reminder time may not be configured.
    val reminderTime: String?,

    // Stores progress values using dates as keys.
    // The Double value represents the amount of progress
    // recorded for each date.
    val progressLog: Map<String, Double>?,

    // Timestamp indicating when the goal was created.
    val createdAt: Long,

    // Timestamp indicating when the goal was last updated.
    val updatedAt: Long
)


 // Request object used to create a new goal or edit an
 // existing tracked goal.
data class SaveTrackedGoalRequest(

    // Category to which the goal belongs.
    val category: String,

    // Name of the goal.
    val name: String,

    // Target value the user wants to achieve.
    val targetValue: Double,

    // Unit used to measure progress towards the goal.
    val unit: String,

    // Frequency at which the goal should be completed.
    val frequency: String,

    // Determines whether reminders are enabled.
    val remindersEnabled: Boolean,

    // Time at which the reminder should be triggered.
    val reminderTime: String
)

// Request object used when recording progress towards a goal.
data class LogProgressRequest(

    // Date on which the progress was recorded.
    val date: String,

    // Amount of progress recorded for the specified date.
    val value: Double
)

// ------------------ END OF FILE ------------------ //