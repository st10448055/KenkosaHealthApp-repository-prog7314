package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID

private const val GOALS_COLLECTION = "goals"

class GoalRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getGoalsForUser(userId: String): List<GoalResponse> {
        val snapshot = db.collection(GOALS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toGoalResponse() }
    }

    suspend fun getGoal(goalId: String): GoalResponse? =
        db.collection(GOALS_COLLECTION).document(goalId).get().await().toGoalResponse()

    suspend fun createGoal(userId: String, request: CreateGoalRequest): GoalResponse {
        val goalId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "goalId" to goalId,
            "userId" to userId,
            "category" to request.category.name,
            "name" to request.name.trim(),
            "targetValue" to request.targetValue,
            "currentValue" to 0.0,
            "unit" to request.unit.trim(),
            "frequency" to request.frequency.name,
            "remindersEnabled" to request.remindersEnabled,
            "weeklyStreak" to List(7) { false },
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection(GOALS_COLLECTION).document(goalId).set(data).await()
        return getGoal(goalId)!!
    }

    suspend fun updateGoal(goalId: String, request: UpdateGoalRequest): GoalResponse? {
        val docRef = db.collection(GOALS_COLLECTION).document(goalId)
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        request.currentValue?.let { updates["currentValue"] = it }
        request.targetValue?.let { updates["targetValue"] = it }
        request.name?.let { updates["name"] = it.trim() }
        request.remindersEnabled?.let { updates["remindersEnabled"] = it }
        docRef.update(updates).await()
        return getGoal(goalId)
    }

    suspend fun deleteGoal(goalId: String) {
        db.collection(GOALS_COLLECTION).document(goalId).delete().await()
    }

    private fun DocumentSnapshot.toGoalResponse(): GoalResponse? {
        if (!exists()) return null
        return GoalResponse(
            goalId = getString("goalId") ?: id,
            userId = getString("userId") ?: return null,
            category = runCatching { GoalCategory.valueOf(getString("category") ?: "CUSTOM") }
                .getOrDefault(GoalCategory.CUSTOM),
            name = getString("name") ?: "",
            targetValue = getDouble("targetValue") ?: 0.0,
            currentValue = getDouble("currentValue") ?: 0.0,
            unit = getString("unit") ?: "",
            frequency = runCatching { GoalFrequency.valueOf(getString("frequency") ?: "DAILY") }
                .getOrDefault(GoalFrequency.DAILY),
            remindersEnabled = getBoolean("remindersEnabled") ?: false,
            weeklyStreak = (get("weeklyStreak") as? List<*>)?.map { it as? Boolean ?: false }
                ?: List(7) { false },
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}