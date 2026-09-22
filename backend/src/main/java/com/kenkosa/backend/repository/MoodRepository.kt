package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID

// Firestore collection used to store mood entries.
private const val MOODS_COLLECTION = "moods"

class MoodRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Retrieves all mood entries belonging to a specific user.
    suspend fun getMoodsForUser(userId: String): List<MoodEntryResponse> {
        val snapshot = db.collection(MOODS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Convert the documents and show the most recently logged moods first.
        return snapshot.documents
            .mapNotNull { it.toMoodEntryResponse() }
            .sortedByDescending { it.loggedAt }
    }

    // Retrieves a specific mood entry by its ID.
    suspend fun getMood(moodId: String): MoodEntryResponse? =
        db.collection(MOODS_COLLECTION).document(moodId).get().await().toMoodEntryResponse()

    // Creates and stores a new mood entry.
    suspend fun createMood(userId: String, request: CreateMoodRequest): MoodEntryResponse {
        val moodId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "moodId" to moodId,
            "userId" to userId,
            "mood" to request.mood.name,
            "feelings" to request.feelings.distinct().map { it.name },
            "loggedAt" to request.loggedAt,
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection(MOODS_COLLECTION).document(moodId).set(data).await()
        return getMood(moodId)!!
    }

    // Updates the mood details provided in the request.
    suspend fun updateMood(moodId: String, request: UpdateMoodRequest): MoodEntryResponse? {
        val docRef = db.collection(MOODS_COLLECTION).document(moodId)
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        request.mood?.let { updates["mood"] = it.name }
        request.feelings?.let { list -> updates["feelings"] = list.distinct().map { it.name } }
        request.loggedAt?.let { updates["loggedAt"] = it }
        docRef.update(updates).await()
        return getMood(moodId)
    }

    // Deletes a mood entry from Firestore.
    suspend fun deleteMood(moodId: String) {
        db.collection(MOODS_COLLECTION).document(moodId).delete().await()
    }

    // Converts a Firestore document into a MoodEntryResponse.
    private fun DocumentSnapshot.toMoodEntryResponse(): MoodEntryResponse? {
        if (!exists()) return null
        return MoodEntryResponse(
            moodId = getString("moodId") ?: id,
            userId = getString("userId") ?: return null,
            mood = runCatching { MoodLevel.valueOf(getString("mood") ?: "NEUTRAL") }
                .getOrDefault(MoodLevel.NEUTRAL),
            feelings = (get("feelings") as? List<*>)
                ?.mapNotNull { name -> runCatching { MoodFeeling.valueOf(name as String) }.getOrNull() }
                ?: emptyList(),
            loggedAt = getLong("loggedAt") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}

// ------------------ END OF FILE ------------------ //