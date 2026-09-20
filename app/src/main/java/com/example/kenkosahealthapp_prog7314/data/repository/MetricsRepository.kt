package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val SETTINGS_COLLECTION = "userSettings"

class MetricsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** The user's saved metrics, or null if they haven't completed them yet. */
    suspend fun getMetrics(userId: String): UserMetrics? {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return UserMetrics.fromSnapshot(db.collection(USERS_COLLECTION).document(userId).get().await())
    }

    /** "Metric", "Imperial" or "UK" from the user's Settings, or "Metric" if they haven't chosen. */
    suspend fun getUnitSystem(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val doc = db.collection(SETTINGS_COLLECTION).document(userId).get().await()
        return doc.getString("unitSystem") ?: "Metric"
    }

    /** Merges into users/{uid}, so name, email and the other profile fields are left untouched. */
    suspend fun saveMetrics(userId: String, metrics: UserMetrics) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        db.collection(USERS_COLLECTION).document(userId)
            .set(metrics.toFirestoreMap(), SetOptions.merge())
            .await()
    }
}
