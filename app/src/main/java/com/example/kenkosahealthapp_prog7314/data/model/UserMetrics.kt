package com.example.kenkosahealthapp_prog7314.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

enum class Gender { FEMALE, MALE, NON_BINARY, PREFER_NOT_TO_SAY }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, EXTRA_ACTIVE }

enum class HeightUnit { CM, FT_IN }

enum class WeightUnit { KG, LB }

/** Sane bounds used by the form so typos (or unit mix-ups) are caught before saving. */
object MetricsLimits {
    const val MIN_HEIGHT_CM = 50.0
    const val MAX_HEIGHT_CM = 250.0
    const val MIN_WEIGHT_KG = 20.0
    const val MAX_WEIGHT_KG = 500.0
    const val MAX_AGE_YEARS = 120
}

object UnitConversions {
    private const val CM_PER_INCH = 2.54
    private const val KG_PER_LB = 0.45359237

    fun ftInToCm(feet: Int, inches: Double): Double = (feet * 12 + inches) * CM_PER_INCH

    /** Splits a height in cm into whole feet and remaining inches (1 decimal), e.g. 177.8 -> 5 ft 10 in. */
    fun cmToFtIn(cm: Double): Pair<Int, Double> {
        val totalInches = Math.round(cm / CM_PER_INCH * 10.0) / 10.0
        var feet = (totalInches / 12).toInt()
        var inches = Math.round((totalInches - feet * 12) * 10.0) / 10.0
        if (inches >= 12.0) { // e.g. 11.96 in rounds up to a whole foot
            feet += 1
            inches = 0.0
        }
        return feet to inches
    }

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB
}

/**
 * The body metrics collected on first login and editable from Settings. Values are always stored in
 * cm / kg; [heightUnit] and [weightUnit] only remember how the user prefers to see and enter them.
 *
 * Lives on the existing users/{uid} document (dateOfBirth, gender, heightCm and weightKg are the fields
 * User.kt already reserved for this).
 */
data class UserMetrics(
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val weightUnit: WeightUnit = WeightUnit.KG
) {
    /** Age is worked out from the date of birth, so it can never go stale or disagree with it. */
    val age: Int get() = Period.between(dateOfBirth, LocalDate.now()).years

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        // Midnight UTC keeps the calendar date identical in every time zone.
        "dateOfBirth" to dateOfBirth.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        "gender" to gender.name,
        "heightCm" to heightCm,
        "weightKg" to weightKg,
        "activityLevel" to activityLevel.name,
        "heightUnit" to heightUnit.name,
        "weightUnit" to weightUnit.name,
        "metricsUpdatedAt" to System.currentTimeMillis()
    )

    companion object {
        /**
         * Returns the metrics stored on a users/{uid} document, or null if any required answer is
         * missing. Null is what triggers the first-login prompt.
         */
        fun fromSnapshot(doc: DocumentSnapshot): UserMetrics? {
            if (!doc.exists()) return null

            val dobMillis = (doc.get("dateOfBirth") as? Number)?.toLong() ?: return null
            val gender = enumOrNull<Gender>(doc.get("gender") as? String) ?: return null
            val heightCm = (doc.get("heightCm") as? Number)?.toDouble() ?: return null
            val weightKg = (doc.get("weightKg") as? Number)?.toDouble() ?: return null
            val activity = enumOrNull<ActivityLevel>(doc.get("activityLevel") as? String) ?: return null

            return UserMetrics(
                dateOfBirth = Instant.ofEpochMilli(dobMillis).atZone(ZoneOffset.UTC).toLocalDate(),
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activity,
                heightUnit = enumOrNull<HeightUnit>(doc.get("heightUnit") as? String) ?: HeightUnit.CM,
                weightUnit = enumOrNull<WeightUnit>(doc.get("weightUnit") as? String) ?: WeightUnit.KG
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
    }
}
