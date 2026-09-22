package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.CreateMealEntryRequest
import com.kenkosa.backend.models.MealEntryResponse
import java.util.UUID

// Firestore collection used to store meal entries.
private const val MEAL_ENTRIES_COLLECTION = "mealEntries"

class MealDiaryRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Retrieves all meals for a user, optionally filtered by date.
    suspend fun getMealsForUser(userId: String, date: String?): List<MealEntryResponse> {
        var query = db.collection(MEAL_ENTRIES_COLLECTION).whereEqualTo("userId", userId)
        if (date != null) {
            query = query.whereEqualTo("date", date)
        }

        val snapshot = query.get().await()

        // Convert the documents and show the most recently logged meals first.
        return snapshot.documents.mapNotNull { it.toMealEntryResponse() }
            .sortedByDescending { it.loggedAt }
    }

    // Retrieves a specific meal entry by its ID.
    suspend fun getMealEntry(mealId: String): MealEntryResponse? =
        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).get().await().toMealEntryResponse()

    // Creates a new meal entry and calculates its nutrition based on servings.
    suspend fun createMealEntry(userId: String, request: CreateMealEntryRequest): MealEntryResponse {
        val mealId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Scale the nutritional values according to the number of servings.
        val scaledCalories = request.baseCalories * request.servings
        val scaledProtein = request.baseProteinG * request.servings
        val scaledCarbs = request.baseCarbsG * request.servings
        val scaledFat = request.baseFatG * request.servings

        val data = mapOf(
            "mealId" to mealId,
            "userId" to userId,
            "date" to request.date,
            "mealType" to request.mealType,
            "foodName" to request.foodName,
            "servings" to request.servings,
            "baseCalories" to request.baseCalories,
            "baseProteinG" to request.baseProteinG,
            "baseCarbsG" to request.baseCarbsG,
            "baseFatG" to request.baseFatG,
            "calories" to scaledCalories,
            "proteinG" to scaledProtein,
            "carbsG" to scaledCarbs,
            "fatG" to scaledFat,
            "loggedAt" to now
        )

        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).set(data).await()
        return getMealEntry(mealId)!!
    }

    // Updates the serving size and recalculates the nutritional values.
    suspend fun updateMealEntryServings(mealId: String, newServings: Double): MealEntryResponse? {
        val existing = getMealEntry(mealId) ?: return null

        val scaledCalories = existing.baseCalories * newServings
        val scaledProtein = existing.baseProteinG * newServings
        val scaledCarbs = existing.baseCarbsG * newServings
        val scaledFat = existing.baseFatG * newServings

        val updates = mapOf(
            "servings" to newServings,
            "calories" to scaledCalories,
            "proteinG" to scaledProtein,
            "carbsG" to scaledCarbs,
            "fatG" to scaledFat
        )

        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).update(updates).await()
        return getMealEntry(mealId)
    }

    // Deletes a meal entry and returns whether it existed.
    suspend fun deleteMealEntry(mealId: String): Boolean {
        val existing = getMealEntry(mealId) ?: return false
        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).delete().await()
        return true
    }

    // Converts a Firestore document into a MealEntryResponse.
    private fun DocumentSnapshot.toMealEntryResponse(): MealEntryResponse? {
        if (!exists()) return null
        return MealEntryResponse(
            mealId = getString("mealId") ?: id,
            userId = getString("userId") ?: return null,
            date = getString("date") ?: return null,
            mealType = getString("mealType") ?: return null,
            foodName = getString("foodName") ?: return null,
            servings = getDouble("servings") ?: 1.0,
            baseCalories = getDouble("baseCalories") ?: 0.0,
            baseProteinG = getDouble("baseProteinG") ?: 0.0,
            baseCarbsG = getDouble("baseCarbsG") ?: 0.0,
            baseFatG = getDouble("baseFatG") ?: 0.0,
            calories = getDouble("calories") ?: 0.0,
            proteinG = getDouble("proteinG") ?: 0.0,
            carbsG = getDouble("carbsG") ?: 0.0,
            fatG = getDouble("fatG") ?: 0.0,
            loggedAt = getLong("loggedAt") ?: 0L
        )
    }
}

// ------------------ END OF FILE ------------------ //