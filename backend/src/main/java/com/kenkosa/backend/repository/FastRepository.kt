package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID

private const val FASTS_COLLECTION = "fasts"

class FastRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getFastsForUser(userId: String): List<FastResponse> {
        val snapshot = db.collection(FASTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toFastResponse() }
            .sortedByDescending { it.startTime }
    }

    suspend fun getFast(fastId: String): FastResponse? =
        db.collection(FASTS_COLLECTION).document(fastId).get().await().toFastResponse()

    /** True if the user already has a fast in progress (optionally ignoring one fast, e.g. the one being edited). */
    suspend fun hasActiveFast(userId: String, exceptFastId: String? = null): Boolean =
        getFastsForUser(userId).any { it.active && it.fastId != exceptFastId }

    suspend fun createFast(userId: String, request: CreateFastRequest): FastResponse {
        val fastId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "fastId" to fastId,
            "userId" to userId,
            "startTime" to request.startTime,
            "endTime" to request.endTime,
            "active" to request.isActive(),
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection(FASTS_COLLECTION).document(fastId).set(data).await()
        return getFast(fastId)!!
    }

    suspend fun updateFast(fastId: String, startTime: Long, endTime: Long, active: Boolean): FastResponse? {
        val updates = mapOf(
            "startTime" to startTime,
            "endTime" to endTime,
            "active" to active,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection(FASTS_COLLECTION).document(fastId).update(updates).await()
        return getFast(fastId)
    }

    suspend fun deleteFast(fastId: String) {
        db.collection(FASTS_COLLECTION).document(fastId).delete().await()
    }

    private fun DocumentSnapshot.toFastResponse(): FastResponse? {
        if (!exists()) return null
        return FastResponse(
            fastId = getString("fastId") ?: id,
            userId = getString("userId") ?: return null,
            startTime = getLong("startTime") ?: 0L,
            endTime = getLong("endTime") ?: 0L,
            active = getBoolean("active") ?: false,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}