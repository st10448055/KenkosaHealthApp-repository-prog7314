package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.SaveSleepLogRequest
import com.kenkosa.backend.models.SleepLogResponse
import java.util.UUID

// Same collection as the original sleep endpoint on purpose, so existing records are kept.
private const val SLEEP_LOG_COLLECTION = "sleepRecords"

class SleepLogRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Retrieves all sleep records belonging to a specific user.
    suspend fun getSleepForUser(userId: String): List<SleepLogResponse> {
        val snapshot = db.collection(SLEEP_LOG_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Convert the documents and show the most recent sleep records first.
        return snapshot.documents
            .mapNotNull { it.toSleepLog() }
            .sortedByDescending { it.sleepEnd }
    }

    // Retrieves a specific sleep record by its ID.
    suspend fun getSleep(sleepId: String): SleepLogResponse? =
        db.collection(SLEEP_LOG_COLLECTION).document(sleepId).get().await().toSleepLog()

    // Creates and stores a new sleep record.
    suspend fun createSleep(userId: String, request: SaveSleepLogRequest): SleepLogResponse {
        val sleepId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "sleepId" to sleepId,
            "userId" to userId,
            "sleepStart" to request.sleepStart,
            "sleepEnd" to request.sleepEnd,
            "totalDurationMinutes" to minutesBetween(request.sleepStart, request.sleepEnd),
            "qualityRating" to request.qualityRating,
            "qualityScore" to request.qualityRating * 20,
            "notes" to request.notes?.trim().takeUnless { it.isNullOrEmpty() },
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection(SLEEP_LOG_COLLECTION).document(sleepId).set(data).await()
        return getSleep(sleepId)!!
    }

    // Updates the sleep times, quality rating, and notes.
    suspend fun updateSleep(sleepId: String, request: SaveSleepLogRequest): SleepLogResponse? {
        val updates = mapOf(
            "sleepStart" to request.sleepStart,
            "sleepEnd" to request.sleepEnd,
            "totalDurationMinutes" to minutesBetween(request.sleepStart, request.sleepEnd),
            "qualityRating" to request.qualityRating,
            "qualityScore" to request.qualityRating * 20,
            "notes" to request.notes?.trim().takeUnless { it.isNullOrEmpty() },
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection(SLEEP_LOG_COLLECTION).document(sleepId).update(updates).await()
        return getSleep(sleepId)
    }

    // Deletes a sleep record from Firestore.
    suspend fun deleteSleep(sleepId: String) {
        db.collection(SLEEP_LOG_COLLECTION).document(sleepId).delete().await()
    }

    // Calculates the sleep duration in minutes.
    private fun minutesBetween(start: Long, end: Long): Int = ((end - start) / 60_000L).toInt()

    // Converts a Firestore document into a SleepLogResponse.
    private fun DocumentSnapshot.toSleepLog(): SleepLogResponse? {
        if (!exists()) return null
        val created = getLong("createdAt") ?: 0L
        return SleepLogResponse(
            sleepId = getString("sleepId") ?: id,
            userId = getString("userId") ?: return null,
            sleepStart = getLong("sleepStart") ?: 0L,
            sleepEnd = getLong("sleepEnd") ?: 0L,
            totalDurationMinutes = (getLong("totalDurationMinutes") ?: 0L).toInt(),
            qualityRating = (getLong("qualityRating") ?: 0L).toInt(),
            qualityScore = (getLong("qualityScore") ?: 0L).toInt(),
            notes = getString("notes"),
            createdAt = created,
            updatedAt = getLong("updatedAt") ?: created
        )
    }
}

// ------------------ END OF FILE ------------------ //