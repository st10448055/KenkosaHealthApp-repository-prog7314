package com.example.kenkosahealthapp_prog7314.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import kotlinx.coroutines.tasks.await

// Firestore collection containing user profile documents.
private const val USERS_COLLECTION = "users"

// Firestore collection containing user settings documents.
private const val SETTINGS_COLLECTION = "userSettings"

// Repository responsible for creating, retrieving and updating
// user profile information and application settings in Firestore.
class UserRepository {

    // Firebase Firestore database instance used for storing user data.
    private val db = FirebaseFirestore.getInstance()

    // Creates a new user document in the users collection.
    suspend fun createUserDocument(
        user: User
    ) {

        // Ensures that the Firebase user ID is not empty.
        require(user.userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Saves the User object to users/{userId} in Firestore.
        db.collection(USERS_COLLECTION)
            .document(user.userId)
            .set(user)
            .await()
    }

    // Retrieves a user's profile from Firestore.
    // Returns null if the supplied user ID is blank or no user is found.
    suspend fun getUser(
        userId: String
    ): User? {

        // Return null when no valid user ID is supplied.
        if (userId.isBlank()) return null

        // Retrieves the user document and converts it
        // into a User object.
        return db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
            .toObject(User::class.java)
    }

    // Updates the user's name and surname.
    suspend fun updateUserName(
        userId: String,
        name: String,
        surname: String
    ) {

        // Ensures that the Firebase user ID is not empty.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Updates only the name and surname fields.
        // SetOptions.merge() prevents other profile information
        // from being overwritten.
        db.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                mapOf(
                    "name" to name,
                    "surname" to surname
                ),
                SetOptions.merge()
            )
            .await()
    }

    /** Merges any profile fields (name, gender, heightCm, weightKg ...) into users/{uid}. */
    suspend fun updateProfile(userId: String, fields: Map<String, Any?>) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        db.collection(USERS_COLLECTION).document(userId)
            .set(fields, SetOptions.merge())
            .await()
    }

    // Creates the user's settings document using the default settings.
    suspend fun createDefaultSettings(
        userId: String
    ) {

        // Ensures that the Firebase user ID is not empty.
        require(userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Creates a UserSettings object containing the application's
        // default settings for the user.
        val defaults = UserSettings(
            userId = userId
        )

        // Saves the default settings to userSettings/{userId}.
        db.collection(SETTINGS_COLLECTION)
            .document(userId)
            .set(defaults)
            .await()
    }

    // Retrieves the user's application settings from Firestore.
    // Returns null if the supplied user ID is blank or no settings are found.
    suspend fun getSettings(
        userId: String
    ): UserSettings? {

        // Return null when no valid user ID is supplied.
        if (userId.isBlank()) return null

        // Retrieves the settings document and converts it
        // into a UserSettings object.
        return db.collection(SETTINGS_COLLECTION)
            .document(userId)
            .get()
            .await()
            .toObject(UserSettings::class.java)
    }

    // Updates the user's application settings.
    suspend fun updateSettings(
        settings: UserSettings
    ) {

        // Ensures that the settings object contains a valid user ID.
        require(settings.userId.isNotBlank()) {
            "User ID cannot be blank"
        }

        // Saves the updated settings to the user's Firestore document.
        db.collection(SETTINGS_COLLECTION)
            .document(settings.userId)
            .set(settings)
            .await()
    }
}

// ------------------ END OF FILE ------------------ //