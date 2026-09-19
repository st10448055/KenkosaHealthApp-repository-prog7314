package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID

private const val MOODS_COLLECTION = "moods"

class MoodRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getMoodsForUser(userId: String): List<MoodEntryResponse> {
        val snapshot = db.collection(MOODS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toMoodEntryResponse() }
            .sortedByDescending { it.loggedAt }
    }

    suspend fun getMood(moodId: String): MoodEntryResponse? =
        db.collection(MOODS_COLLECTION).document(moodId).get().await().toMoodEntryResponse()

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

    suspend fun updateMood(moodId: String, request: UpdateMoodRequest): MoodEntryResponse? {
        val docRef = db.collection(MOODS_COLLECTION).document(moodId)
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        request.mood?.let { updates["mood"] = it.name }
        request.feelings?.let { list -> updates["feelings"] = list.distinct().map { it.name } }
        request.loggedAt?.let { updates["loggedAt"] = it }
        docRef.update(updates).await()
        return getMood(moodId)
    }

    suspend fun deleteMood(moodId: String) {
        db.collection(MOODS_COLLECTION).document(moodId).delete().await()
    }

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
