package com.example.kenkosahealthapp_prog7314.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val SETTINGS_COLLECTION = "userSettings"

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createUserDocument(user: User) {
        db.collection(USERS_COLLECTION).document(user.userId).set(user.toMap()).await()
    }

    suspend fun getUser(userId: String): User? =
        db.collection(USERS_COLLECTION).document(userId).get().await().toObject(User::class.java)

    suspend fun createDefaultSettings(userId: String) {
        val defaults = UserSettings(userId = userId)
        db.collection(SETTINGS_COLLECTION).document(userId).set(defaults.toMap()).await()
    }

    suspend fun getSettings(userId: String): UserSettings? =
        db.collection(SETTINGS_COLLECTION).document(userId).get().await()
            .toObject(UserSettings::class.java)

    suspend fun updateSettings(settings: UserSettings) {
        db.collection(SETTINGS_COLLECTION).document(settings.userId)
            .set(settings.toMap()).await()
    }
}