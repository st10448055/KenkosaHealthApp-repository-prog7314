package com.kenkosa.backend.repository

import com.google.cloud.firestore.SetOptions
import com.google.firebase.cloud.FirestoreClient

private const val DEVICE_TOKENS_COLLECTION = "deviceTokens"

// The document id IS the token, so a token can only belong to one user at a time.
// If someone else signs in on the same phone, the document simply changes owner.
class DeviceTokenRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun register(userId: String, token: String, platform: String) {
        val data = mapOf(
            "userId" to userId,
            "token" to token,
            "platform" to platform,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection(DEVICE_TOKENS_COLLECTION).document(token).set(data, SetOptions.merge()).await()
    }

    // Removes a token, but only if it belongs to the caller.
    suspend fun unregister(userId: String, token: String) {
        val ref = db.collection(DEVICE_TOKENS_COLLECTION).document(token)
        val doc = ref.get().await()
        if (doc.exists() && doc.getString("userId") == userId) ref.delete().await()
    }

    suspend fun tokensFor(userId: String): List<String> =
        db.collection(DEVICE_TOKENS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get().await()
            .documents
            .mapNotNull { it.getString("token") }

    // Deletes tokens Firebase says are dead (app uninstalled, token rotated).
    suspend fun removeAll(tokens: Collection<String>) {
        tokens.forEach { db.collection(DEVICE_TOKENS_COLLECTION).document(it).delete().await() }
    }
}