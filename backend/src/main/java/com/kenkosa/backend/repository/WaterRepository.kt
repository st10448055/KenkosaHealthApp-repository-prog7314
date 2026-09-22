package com.kenkosa.backend.repository

import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.WaterIntakeResponse
import java.time.LocalDate

// Firestore collection used to store daily water intake.
private const val WATER_COLLECTION = "waterIntake"

// Default daily water target in cups.
private const val DEFAULT_TARGET_CUPS = 8

class WaterRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    /** One document per user per day, keyed as "{userId}_{date}" for a direct lookup. */
    private fun docId(userId: String, date: String) = "${userId}_$date"

    // Gets today's water intake, or returns default values if nothing has been logged.
    suspend fun getTodaysWater(userId: String, date: String): WaterIntakeResponse {
        val doc = db.collection(WATER_COLLECTION).document(docId(userId, date)).get().await()

        return if (doc.exists()) {
            // Builds the response using the values stored in Firestore.
            WaterIntakeResponse(
                date = date,
                cupsConsumed = (doc.getLong("cupsConsumed") ?: 0L).toInt(),
                targetCups = (doc.getLong("targetCups") ?: DEFAULT_TARGET_CUPS.toLong()).toInt()
            )
        } else {
            // No entry yet today — return a clean zeroed default rather than 404,
            // since "no water logged yet" is a normal state, not an error.
            WaterIntakeResponse(date = date, cupsConsumed = 0, targetCups = DEFAULT_TARGET_CUPS)
        }
    }

    // Saves the user's water intake for the specified day.
    suspend fun updateWater(userId: String, date: String, cupsConsumed: Int): WaterIntakeResponse {
        // Creates the data that will be stored in Firestore.
        val data = mapOf(
            "userId" to userId,
            "date" to date,
            "cupsConsumed" to cupsConsumed,
            "targetCups" to DEFAULT_TARGET_CUPS
        )

        // Stores or replaces the user's daily water record.
        db.collection(WATER_COLLECTION).document(docId(userId, date)).set(data).await()

        // Returns the updated water intake to the caller.
        return WaterIntakeResponse(date = date, cupsConsumed = cupsConsumed, targetCups = DEFAULT_TARGET_CUPS)
    }
}

// ------------------ END OF FILE ------------------ //