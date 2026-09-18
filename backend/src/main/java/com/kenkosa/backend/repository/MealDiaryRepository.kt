package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.CreateMealEntryRequest
import com.kenkosa.backend.models.MealEntryResponse
import java.util.UUID

private const val MEAL_ENTRIES_COLLECTION = "mealEntries"

class MealDiaryRepository {

    private val db get() = FirestoreClient.getFirestore()

    suspend fun getMealsForUser(userId: String, date: String?): List<MealEntryResponse> {
        var query = db.collection(MEAL_ENTRIES_COLLECTION).whereEqualTo("userId", userId)
        if (date != null) {
            query = query.whereEqualTo("date", date)
        }

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { it.toMealEntryResponse() }
            .sortedByDescending { it.loggedAt }
    }

    suspend fun getMealEntry(mealId: String): MealEntryResponse? =
        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).get().await().toMealEntryResponse()

    suspend fun createMealEntry(userId: String, request: CreateMealEntryRequest): MealEntryResponse {
        val mealId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

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
            "calories" to scaledCalories,
            "proteinG" to scaledProtein,
            "carbsG" to scaledCarbs,
            "fatG" to scaledFat,
            "loggedAt" to now
        )

        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).set(data).await()
        return getMealEntry(mealId)!!
    }

    suspend fun deleteMealEntry(mealId: String): Boolean {
        val existing = getMealEntry(mealId) ?: return false
        db.collection(MEAL_ENTRIES_COLLECTION).document(mealId).delete().await()
        return true
    }

    private fun DocumentSnapshot.toMealEntryResponse(): MealEntryResponse? {
        if (!exists()) return null
        return MealEntryResponse(
            mealId = getString("mealId") ?: id,
            userId = getString("userId") ?: return null,
            date = getString("date") ?: return null,
            mealType = getString("mealType") ?: return null,
            foodName = getString("foodName") ?: return null,
            servings = getDouble("servings") ?: 1.0,
            calories = getDouble("calories") ?: 0.0,
            proteinG = getDouble("proteinG") ?: 0.0,
            carbsG = getDouble("carbsG") ?: 0.0,
            fatG = getDouble("fatG") ?: 0.0,
            loggedAt = getLong("loggedAt") ?: 0L
        )
    }
}