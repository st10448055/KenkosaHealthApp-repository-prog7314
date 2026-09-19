package com.example.kenkosahealthapp_prog7314.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val SETTINGS_COLLECTION = "userSettings"

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createUserDocument(user: User) {
        require(user.userId.isNotBlank()) { "User ID cannot be blank" }
        db.collection(USERS_COLLECTION).document(user.userId).set(user).await()
    }

    suspend fun getUser(userId: String): User? {
        if (userId.isBlank()) return null
        return db.collection(USERS_COLLECTION).document(userId).get().await()
            .toObject(User::class.java)
    }

    suspend fun updateUserName(userId: String, name: String, surname: String) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        db.collection(USERS_COLLECTION).document(userId)
            .set(mapOf("name" to name, "surname" to surname), SetOptions.merge())
            .await()
    }

    suspend fun createDefaultSettings(userId: String) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val defaults = UserSettings(userId = userId)
        db.collection(SETTINGS_COLLECTION).document(userId).set(defaults).await()
    }

    suspend fun getSettings(userId: String): UserSettings? {
        if (userId.isBlank()) return null
        return db.collection(SETTINGS_COLLECTION).document(userId).get().await()
            .toObject(UserSettings::class.java)
    }

    suspend fun updateSettings(settings: UserSettings) {
        require(settings.userId.isNotBlank()) { "User ID cannot be blank" }
        db.collection(SETTINGS_COLLECTION).document(settings.userId)
            .set(settings).await()
    }
}
