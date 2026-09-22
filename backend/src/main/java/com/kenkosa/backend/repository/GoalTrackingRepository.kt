package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.FieldPath
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID


private const val GOALS_COLLECTION = "goals"

class GoalTrackingRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getGoalsForUser(userId: String): List<TrackedGoalResponse> {
        val snapshot = db.collection(GOALS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toTrackedGoal() }
            .sortedBy { it.createdAt }
    }

    suspend fun getGoal(goalId: String): TrackedGoalResponse? =
        db.collection(GOALS_COLLECTION).document(goalId).get().await().toTrackedGoal()

    suspend fun createGoal(userId: String, request: SaveTrackedGoalRequest): TrackedGoalResponse {
        val goalId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "goalId" to goalId,
            "userId" to userId,
            "category" to request.category.name,
            "name" to request.name.trim(),
            "targetValue" to request.targetValue,
            "unit" to request.unit.trim(),
            "frequency" to request.frequency.name,
            "remindersEnabled" to request.remindersEnabled,
            "reminderTime" to request.reminderTime,
            "progressLog" to emptyMap<String, Double>(),
            // Fields the older goal models still expect to find:
            "currentValue" to 0.0,
            "weeklyStreak" to List(7) { false },
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection(GOALS_COLLECTION).document(goalId).set(data).await()
        return getGoal(goalId)!!
    }

    suspend fun updateGoal(goalId: String, request: SaveTrackedGoalRequest): TrackedGoalResponse? {
        val updates = mapOf<String, Any>(
            "category" to request.category.name,
            "name" to request.name.trim(),
            "targetValue" to request.targetValue,
            "unit" to request.unit.trim(),
            "frequency" to request.frequency.name,
            "remindersEnabled" to request.remindersEnabled,
            "reminderTime" to request.reminderTime,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection(GOALS_COLLECTION).document(goalId).update(updates).await()
        return getGoal(goalId)
    }


    suspend fun logProgress(goalId: String, date: String, value: Double): TrackedGoalResponse? {
        db.collection(GOALS_COLLECTION).document(goalId).update(
            FieldPath.of("progressLog", date), value,
            FieldPath.of("currentValue"), value,
            FieldPath.of("updatedAt"), System.currentTimeMillis()
        ).await()
        return getGoal(goalId)
    }

    suspend fun deleteGoal(goalId: String) {
        db.collection(GOALS_COLLECTION).document(goalId).delete().await()
    }

    private fun DocumentSnapshot.toTrackedGoal(): TrackedGoalResponse? {
        if (!exists()) return null

        val log = (get("progressLog") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val date = key as? String
                val number = (value as? Number)?.toDouble()
                if (date != null && number != null) date to number else null
            }
            ?.toMap()
            ?: emptyMap()

        return TrackedGoalResponse(
            goalId = getString("goalId") ?: id,
            userId = getString("userId") ?: return null,
            category = runCatching { TrackedGoalCategory.valueOf(getString("category") ?: "CUSTOM") }
                .getOrDefault(TrackedGoalCategory.CUSTOM),
            name = getString("name") ?: "",
            targetValue = getDouble("targetValue") ?: 0.0,
            unit = getString("unit") ?: "",
            frequency = runCatching { TrackedGoalFrequency.valueOf(getString("frequency") ?: "DAILY") }
                .getOrDefault(TrackedGoalFrequency.DAILY),
            remindersEnabled = getBoolean("remindersEnabled") ?: false,
            reminderTime = getString("reminderTime") ?: "06:00",
            progressLog = log,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}