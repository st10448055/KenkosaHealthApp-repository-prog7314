package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.FieldPath
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID


// Firestore collection used to store tracked goals.
private const val GOALS_COLLECTION = "goals"

class GoalTrackingRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Retrieves all goals belonging to a specific user.
    suspend fun getGoalsForUser(userId: String): List<TrackedGoalResponse> {
        val snapshot = db.collection(GOALS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Convert the documents to goals and sort them by creation date.
        return snapshot.documents
            .mapNotNull { it.toTrackedGoal() }
            .sortedBy { it.createdAt }
    }

    // Retrieves a specific goal using its ID.
    suspend fun getGoal(goalId: String): TrackedGoalResponse? =
        db.collection(GOALS_COLLECTION).document(goalId).get().await().toTrackedGoal()

    // Creates a new goal and stores it in Firestore.
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

    // Updates the main details of an existing goal.
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


    // Records progress for a specific date and updates the current value.
    suspend fun logProgress(goalId: String, date: String, value: Double): TrackedGoalResponse? {
        db.collection(GOALS_COLLECTION).document(goalId).update(
            FieldPath.of("progressLog", date), value,
            FieldPath.of("currentValue"), value,
            FieldPath.of("updatedAt"), System.currentTimeMillis()
        ).await()
        return getGoal(goalId)
    }

    // Deletes a goal from Firestore.
    suspend fun deleteGoal(goalId: String) {
        db.collection(GOALS_COLLECTION).document(goalId).delete().await()
    }

    // Converts a Firestore document into a TrackedGoalResponse.
    private fun DocumentSnapshot.toTrackedGoal(): TrackedGoalResponse? {
        if (!exists()) return null

        // Converts the stored progress log into a map of dates and values.
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

// ------------------ END OF FILE ------------------ //