package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.CreateSleepRequest
import com.kenkosa.backend.models.SleepRecordResponse
import java.util.UUID

private const val SLEEP_COLLECTION = "sleepRecords"

class SleepRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getSleepForUser(userId: String, from: Long?, to: Long?): List<SleepRecordResponse> {
        var query = db.collection(SLEEP_COLLECTION).whereEqualTo("userId", userId)
        if (from != null) query = query.whereGreaterThanOrEqualTo("sleepStart", from)
        if (to != null) query = query.whereLessThanOrEqualTo("sleepStart", to)

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { it.toSleepResponse() }
            .sortedByDescending { it.sleepStart }
    }

    suspend fun getSleepRecord(sleepId: String): SleepRecordResponse? =
        db.collection(SLEEP_COLLECTION).document(sleepId).get().await().toSleepResponse()

    suspend fun createSleepRecord(userId: String, request: CreateSleepRequest): SleepRecordResponse {
        val sleepId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Handle overnight sleep: if end <= start, assume it rolled past midnight.
        val effectiveEnd = if (request.sleepEnd <= request.sleepStart)
            request.sleepEnd + (24 * 60 * 60 * 1000L)
        else request.sleepEnd

        val durationMinutes = ((effectiveEnd - request.sleepStart) / 60000L).toInt()
        val qualityScore = request.qualityRating * 20

        val data = mapOf(
            "sleepId" to sleepId,
            "userId" to userId,
            "sleepStart" to request.sleepStart,
            "sleepEnd" to effectiveEnd,
            "totalDurationMinutes" to durationMinutes,
            "qualityRating" to request.qualityRating,
            "qualityScore" to qualityScore,
            "notes" to (request.notes?.trim().takeUnless { it.isNullOrEmpty() }),
            "createdAt" to now
        )

        db.collection(SLEEP_COLLECTION).document(sleepId).set(data).await()
        return getSleepRecord(sleepId)!!
    }

    private fun DocumentSnapshot.toSleepResponse(): SleepRecordResponse? {
        if (!exists()) return null
        return SleepRecordResponse(
            sleepId = getString("sleepId") ?: id,
            userId = getString("userId") ?: return null,
            sleepStart = getLong("sleepStart") ?: 0L,
            sleepEnd = getLong("sleepEnd") ?: 0L,
            totalDurationMinutes = (getLong("totalDurationMinutes") ?: 0L).toInt(),
            qualityRating = (getLong("qualityRating") ?: 0L).toInt(),
            qualityScore = (getLong("qualityScore") ?: 0L).toInt(),
            notes = getString("notes"),
            createdAt = getLong("createdAt") ?: 0L
        )
    }
}