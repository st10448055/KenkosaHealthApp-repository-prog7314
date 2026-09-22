package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// Firestore collection containing the user's profile information and metrics.
private const val USERS_COLLECTION = "users"

// Firestore collection containing the user's application settings.
private const val SETTINGS_COLLECTION = "userSettings"

// Repository responsible for reading and saving user health metrics
// and the selected unit system using Firebase Firestore.
class MetricsRepository(
    // Uses the default Firebase Firestore instance unless another instance
    // is supplied, which can also make the repository easier to test.
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Retrieves the user's saved health metrics.
    // Returns null if the metrics have not been completed or saved yet.
    suspend fun getMetrics(userId: String): UserMetrics? {

        // Ensures that a valid Firebase user ID has been supplied.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Retrieves the user's document from the users collection
        // and converts the Firestore document into a UserMetrics object.
        return UserMetrics.fromSnapshot(
            db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
        )
    }

    // Retrieves the unit system selected by the user from Firestore.
    // Supported values include "Metric", "Imperial" and "UK".
    // If no unit system has been saved, Metric is used as the default.
    suspend fun getUnitSystem(userId: String): String {

        // Ensures that a valid Firebase user ID has been supplied.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Retrieves the user's settings document.
        val doc = db.collection(SETTINGS_COLLECTION)
            .document(userId)
            .get()
            .await()

        // Gets the saved unit system or defaults to Metric.
        return doc.getString("unitSystem") ?: "Metric"
    }

    // Saves the selected unit system to the user's Settings document.
    // SetOptions.merge() ensures that existing settings are not overwritten.
    suspend fun setUnitSystem(
        userId: String,
        unitSystem: String
    ) {

        // Ensures that a valid Firebase user ID has been supplied.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Updates only the unitSystem field in the user's settings.
        // Other settings remain unchanged because SetOptions.merge()
        // is used.
        db.collection(SETTINGS_COLLECTION)
            .document(userId)
            .set(
                mapOf("unitSystem" to unitSystem),
                SetOptions.merge()
            )
            .await()
    }

    // Saves the user's health metrics to their users/{uid} Firestore document.
    // SetOptions.merge() ensures that existing profile information such as
    // the user's name and email is not removed or overwritten.
    suspend fun saveMetrics(
        userId: String,
        metrics: UserMetrics
    ) {

        // Ensures that a valid Firebase user ID has been supplied.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Converts the UserMetrics object into a Firestore-compatible map
        // and merges the metrics into the existing user document.
        db.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                metrics.toFirestoreMap(),
                SetOptions.merge()
            )
            .await()
    }
}

// ------------------ END OF FILE ------------------ //