package com.example.kenkosahealthapp_prog7314.data.model

data class User(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val surname: String = "",
    val dateOfBirth: Long? = null,
    val gender: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val profileImageUrl: String? = null,
    val isPremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // true for a brand-new account until the user has filled in the body metrics form
    val metricsPending: Boolean = true
) {
    constructor() : this(userId = "")

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "email" to email,
        "name" to name,
        "surname" to surname,
        "dateOfBirth" to dateOfBirth,
        "gender" to gender,
        "heightCm" to heightCm,
        "weightKg" to weightKg,
        "profileImageUrl" to profileImageUrl,
        "isPremium" to isPremium,
        "createdAt" to createdAt,
        "metricsPending" to metricsPending
    )
}