package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// FCM registration tokens are URL-safe strings of letters, digits, ':', '_' and '-'.
// Checking the shape also keeps odd values (like "/" or "..") out of Firestore document ids.
private val FCM_TOKEN_REGEX = Regex("^[A-Za-z0-9:_.\\-]{20,512}$")

// Sent by the app after sign-in (and whenever Firebase rotates the token).
@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String = "android"
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (!FCM_TOKEN_REGEX.matches(token)) errors.add("Invalid device token")
        if (platform != "android") errors.add("Unsupported platform")
        return errors
    }
}

// Sent by the app when notifications are switched off or the user signs out.
@Serializable
data class UnregisterDeviceRequest(val token: String) {
    fun validate(): List<String> =
        if (FCM_TOKEN_REGEX.matches(token)) emptyList() else listOf("Invalid device token")
}

// Result of the "send me a test notification" endpoint.
@Serializable
data class SendResultResponse(val devices: Int, val sent: Int)

// Used by the admin broadcast endpoint. Keys are language codes: "en", "af", "xh".
@Serializable
data class AnnouncementRequest(
    val title: Map<String, String>,
    val body: Map<String, String>,
    val screen: String? = null
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (title["en"].isNullOrBlank()) errors.add("An English title (\"en\") is required")
        if (body["en"].isNullOrBlank()) errors.add("An English body (\"en\") is required")
        if (title.values.any { it.length > 65 }) errors.add("Titles must be 65 characters or fewer")
        if (body.values.any { it.length > 240 }) errors.add("Bodies must be 240 characters or fewer")
        val unknown = (title.keys + body.keys) - NotificationLanguages.SUPPORTED
        if (unknown.isNotEmpty()) errors.add("Unsupported language(s): ${unknown.joinToString()}")
        if (screen != null && screen !in NotificationScreens.ALL) errors.add("Unknown screen \"$screen\"")
        return errors
    }
}

// Result of the admin broadcast endpoint.
@Serializable
data class AnnouncementResult(val sentLanguages: List<String>)

// Languages the app is translated into.
object NotificationLanguages {
    val SUPPORTED = setOf("en", "af", "xh")
    fun normalise(code: String?): String = code?.lowercase()?.takeIf { it in SUPPORTED } ?: "en"
}

// Screens a notification may open. The Android app maps these names to its navigation graph.
object NotificationScreens {
    const val HOME = "home"
    const val WATER = "water"
    const val GOALS = "goals"
    const val SLEEP = "sleep"
    const val FASTING = "fasting"
    const val MEDICATION = "medication"
    const val WELLNESS = "wellness"
    const val SETTINGS = "settings"
    val ALL = setOf(HOME, WATER, GOALS, SLEEP, FASTING, MEDICATION, WELLNESS, SETTINGS)
}