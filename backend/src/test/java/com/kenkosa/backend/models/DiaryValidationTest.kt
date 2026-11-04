package com.kenkosa.backend.models

import kotlin.test.Test
import kotlin.test.assertTrue

class DiaryValidationTest {

    private fun meal(
        date: String = "2026-11-04",
        mealType: String = "lunch",
        foodName: String = "Banana",
        servings: Double = 1.0
    ) = CreateMealEntryRequest(date, mealType, foodName, servings, 89.0, 1.1, 23.0, 0.3)

    @Test
    fun validMeal_hasNoErrors() {
        assertTrue(meal().validate().isEmpty())
    }

    @Test
    fun allFourMealTypes_areAccepted() {
        listOf("breakfast", "lunch", "dinner", "snack").forEach {
            assertTrue(meal(mealType = it).validate().isEmpty(), "$it should be valid")
        }
    }

    @Test
    fun unknownMealType_isRejected() {
        assertTrue(meal(mealType = "brunch").validate().any { it.contains("mealType") })
    }

    @Test
    fun blankFoodName_isRejected() {
        assertTrue(meal(foodName = "  ").validate().any { it.contains("Food name") })
    }

    @Test
    fun blankDate_isRejected() {
        assertTrue(meal(date = "").validate().any { it.contains("Date") })
    }

    @Test
    fun zeroOrNegativeServings_areRejected() {
        assertTrue(meal(servings = 0.0).validate().isNotEmpty())
        assertTrue(meal(servings = -2.0).validate().isNotEmpty())
    }

    @Test
    fun updatingServings_mustBePositive() {
        assertTrue(CreateMealEntryRequest.UpdateMealEntryRequest(1.5).validate().isEmpty())
        assertTrue(CreateMealEntryRequest.UpdateMealEntryRequest(0.0).validate().isNotEmpty())
    }
}