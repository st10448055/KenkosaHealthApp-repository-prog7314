package com.example.kenkosahealthapp_prog7314.data.model

// Data class representing a user's profile and account information.
data class User(

    // Unique identifier for the user, usually obtained from Firebase Authentication.
    val userId: String = "",

    // Email address associated with the user's account.
    val email: String = "",

    // User's first name.
    val name: String = "",

    // User's surname or last name.
    val surname: String = "",

    // User's date of birth stored as a Unix timestamp.
    // Nullable because the user may not have provided their date of birth yet.
    val dateOfBirth: Long? = null,

    // User's selected gender.
    // Nullable because providing a gender is optional.
    val gender: String? = null,

    // User's height in centimetres.
    // Nullable because the user may not have completed their body metrics.
    val heightCm: Double? = null,

    // User's weight in kilograms.
    // Nullable because the user may not have completed their body metrics.
    val weightKg: Double? = null,

    // URL pointing to the user's profile image.
    // Nullable because a profile image is optional.
    val profileImageUrl: String? = null,

    // Indicates whether the user has an active premium account.
    // New users are not premium by default.
    val isPremium: Boolean = false,

    // Stores the time when the user account was created.
    // The value is stored as a Unix timestamp in milliseconds.
    val createdAt: Long = System.currentTimeMillis(),

    // Indicates whether the user still needs to complete their body metrics.
    // New accounts are marked as pending until the required metrics are provided.
    val metricsPending: Boolean = true
) {

    // Empty constructor required by some Firebase Firestore operations
    // when converting Firestore documents into User objects.
    constructor() : this(userId = "")

    // Converts the User object into a Map.
    // This is useful when creating or updating a user document in Firestore.
    fun toMap(): Map<String, Any?> = mapOf(

        // Store the user's unique ID.
        "userId" to userId,

        // Store the user's email address.
        "email" to email,

        // Store the user's first name.
        "name" to name,

        // Store the user's surname.
        "surname" to surname,

        // Store the user's date of birth.
        "dateOfBirth" to dateOfBirth,

        // Store the user's gender.
        "gender" to gender,

        // Store the user's height in centimetres.
        "heightCm" to heightCm,

        // Store the user's weight in kilograms.
        "weightKg" to weightKg,

        // Store the profile image URL.
        "profileImageUrl" to profileImageUrl,

        // Store the user's premium account status.
        "isPremium" to isPremium,

        // Store the account creation timestamp.
        "createdAt" to createdAt,

        // Store whether the user still needs to complete their body metrics.
        "metricsPending" to metricsPending
    )
}
// ------------------ END OF FILE ------------------ //