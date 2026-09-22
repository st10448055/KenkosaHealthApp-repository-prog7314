package com.example.kenkosahealthapp_prog7314.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

// Represents the gender options available when entering user body metrics.
enum class Gender {
    FEMALE,
    MALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY
}

// Represents the user's usual level of physical activity.
enum class ActivityLevel {
    SEDENTARY,
    LIGHT,
    MODERATE,
    VERY_ACTIVE,
    EXTRA_ACTIVE
}

// Units that can be used when entering or displaying height.
enum class HeightUnit {
    CM,
    FT_IN
}

// Units that can be used when entering or displaying weight.
enum class WeightUnit {
    KG,
    LB
}

// Defines sensible limits for height, weight, and age.
// These limits help prevent invalid values, typing mistakes,
// and incorrect values caused by mixing measurement units.
object MetricsLimits {

    // Minimum accepted height in centimetres.
    const val MIN_HEIGHT_CM = 50.0

    // Maximum accepted height in centimetres.
    const val MAX_HEIGHT_CM = 250.0

    // Minimum accepted weight in kilograms.
    const val MIN_WEIGHT_KG = 20.0

    // Maximum accepted weight in kilograms.
    const val MAX_WEIGHT_KG = 500.0

    // Maximum accepted age in years.
    const val MAX_AGE_YEARS = 120
}

// Contains functions for converting between metric and imperial units.
object UnitConversions {

    // Number of centimetres in one inch.
    private const val CM_PER_INCH = 2.54

    // Number of kilograms in one pound.
    private const val KG_PER_LB = 0.45359237

    // Converts a height entered as feet and inches into centimetres.
    fun ftInToCm(feet: Int, inches: Double): Double =
        (feet * 12 + inches) * CM_PER_INCH


     // Converts a height in centimetres into feet and inches.
     // The result contains whole feet and the remaining inches,
     // with the inches rounded to one decimal place.
    fun cmToFtIn(cm: Double): Pair<Int, Double> {

        // Convert centimetres to total inches and round to one decimal place.
        val totalInches =
            Math.round(cm / CM_PER_INCH * 10.0) / 10.0

        // Calculate the number of complete feet.
        var feet = (totalInches / 12).toInt()

        // Calculate the remaining inches after removing the complete feet.
        var inches =
            Math.round((totalInches - feet * 12) * 10.0) / 10.0

        // If rounding causes the inches to reach 12,
        // add another foot and reset the inches to zero.
        if (inches >= 12.0) {
            feet += 1
            inches = 0.0
        }

        // Return the feet and remaining inches as a Pair.
        return feet to inches
    }

    // Converts kilograms to pounds.
    fun kgToLb(kg: Double): Double =
        kg / KG_PER_LB

    // Converts pounds to kilograms.
    fun lbToKg(lb: Double): Double =
        lb * KG_PER_LB
}


 // Stores the body metrics collected during first login
 // and editable later through the Settings screen.
 // Height and weight are always stored internally using
 // centimetres and kilograms. The heightUnit and weightUnit
 // properties only remember the user's preferred display/input units.
 // These values are stored on the existing users/{uid} Firestore
 // document alongside the dateOfBirth, gender, heightCm,
 // and weightKg fields.

data class UserMetrics(

    // User's date of birth.
    val dateOfBirth: LocalDate,

    // User's selected gender.
    val gender: Gender,

    // User's height stored in centimetres.
    val heightCm: Double,

    // User's weight stored in kilograms.
    val weightKg: Double,

    // User's usual level of physical activity.
    val activityLevel: ActivityLevel,

    // Preferred unit for displaying or entering height.
    val heightUnit: HeightUnit = HeightUnit.CM,

    // Preferred unit for displaying or entering weight.
    val weightUnit: WeightUnit = WeightUnit.KG
) {


     // Calculates the user's current age from their date of birth.
     // The age is calculated whenever it is requested instead of
     // being stored, preventing the value from becoming outdated.
    val age: Int
        get() = Period.between(
            dateOfBirth,
            LocalDate.now()
        ).years


     // Converts the UserMetrics object into a Map that can be
     // stored in a Firebase Firestore document.
    fun toFirestoreMap(): Map<String, Any> = mapOf(

        // Store the date of birth as a Unix timestamp in milliseconds.
        // UTC midnight ensures the calendar date remains consistent
        // regardless of the user's time zone.
        "dateOfBirth" to dateOfBirth
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),

        // Store the gender using the enum's name.
        "gender" to gender.name,

        // Store the height in centimetres.
        "heightCm" to heightCm,

        // Store the weight in kilograms.
        "weightKg" to weightKg,

        // Store the activity level using the enum's name.
        "activityLevel" to activityLevel.name,

        // Store the user's preferred height unit.
        "heightUnit" to heightUnit.name,

        // Store the user's preferred weight unit.
        "weightUnit" to weightUnit.name,

        // Store the timestamp of the most recent metrics update.
        "metricsUpdatedAt" to System.currentTimeMillis(),

        // Mark the metrics form as completed.
        // This prevents the first-login metrics prompt from
        // appearing again after the form has been saved.
        "metricsPending" to false
    )

    companion object {


         // Checks whether the user still needs to complete
         // the body metrics form.
         // A newly created account has metricsPending set to true.
         // Older accounts without this field are not prompted.
        fun isPromptPending(doc: DocumentSnapshot): Boolean =
            doc.exists() &&
                    (doc.get("metricsPending") as? Boolean) == true


         // Creates a UserMetrics object from a Firestore document.
         // Returns null if the document does not exist or if any
         // required metric is missing or cannot be converted correctly.
        fun fromSnapshot(doc: DocumentSnapshot): UserMetrics? {

            // Stop if the Firestore document does not exist.
            if (!doc.exists()) return null

            // Retrieve and convert the date of birth timestamp.
            // Return null if the value is missing or invalid.
            val dobMillis =
                (doc.get("dateOfBirth") as? Number)?.toLong()
                    ?: return null

            // Retrieve the stored gender and convert it to the
            // corresponding Gender enum value.
            val gender =
                enumOrNull<Gender>(doc.get("gender") as? String)
                    ?: return null

            // Retrieve the user's height in centimetres.
            val heightCm =
                (doc.get("heightCm") as? Number)?.toDouble()
                    ?: return null

            // Retrieve the user's weight in kilograms.
            val weightKg =
                (doc.get("weightKg") as? Number)?.toDouble()
                    ?: return null

            // Retrieve and convert the user's activity level.
            val activity =
                enumOrNull<ActivityLevel>(
                    doc.get("activityLevel") as? String
                ) ?: return null

            // Create and return a UserMetrics object using
            // the values retrieved from Firestore.
            return UserMetrics(

                // Convert the stored timestamp back into a LocalDate.
                dateOfBirth = Instant
                    .ofEpochMilli(dobMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate(),

                // Set the user's gender.
                gender = gender,

                // Set the user's height in centimetres.
                heightCm = heightCm,

                // Set the user's weight in kilograms.
                weightKg = weightKg,

                // Set the user's activity level.
                activityLevel = activity,

                // Retrieve the preferred height unit.
                // Use centimetres if the field does not exist.
                heightUnit =
                    enumOrNull<HeightUnit>(
                        doc.get("heightUnit") as? String
                    ) ?: HeightUnit.CM,

                // Retrieve the preferred weight unit.
                // Use kilograms if the field does not exist.
                weightUnit =
                    enumOrNull<WeightUnit>(
                        doc.get("weightUnit") as? String
                    ) ?: WeightUnit.KG
            )
        }

         // Safely converts a String into an enum value.
         // If the supplied value is null or does not match
         // an enum value, null is returned instead of throwing
         // an exception.
        private inline fun <reified T : Enum<T>> enumOrNull(
            name: String?
        ): T? =
            name?.let {
                runCatching {
                    enumValueOf<T>(it)
                }.getOrNull()
            }
    }
}

// ------------------ END OF FILE ------------------ //