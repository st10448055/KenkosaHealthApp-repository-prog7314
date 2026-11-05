package com.kenkosa.backend.models

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertTrue

class GoalValidationTest {

    private fun goal(
        category: TrackedGoalCategory = TrackedGoalCategory.WATER,
        name: String = "Drink water",
        target: Double = 8.0,
        unit: String = "glasses",
        reminderTime: String = "06:00"
    ) = SaveTrackedGoalRequest(
        category, name, target, unit, TrackedGoalFrequency.DAILY,
        remindersEnabled = true, reminderTime = reminderTime
    )

    @Test
    fun validGoal_hasNoErrors() {
        assertTrue(goal().validate().isEmpty())
    }

    @Test
    fun blankName_isRejected() {
        assertTrue(goal(name = " ").validate().any { it.contains("name") })
    }

    @Test
    fun zeroTarget_isRejected() {
        assertTrue(goal(target = 0.0).validate().any { it.contains("greater than 0") })
    }

    @Test
    fun sleepTargetOver24Hours_isRejected() {
        assertTrue(goal(category = TrackedGoalCategory.SLEEP, target = 25.0).validate().any { it.contains("24") })
    }

    @Test
    fun badReminderTime_isRejected() {
        assertTrue(goal(reminderTime = "25:00").validate().any { it.contains("HH:mm") })
        assertTrue(goal(reminderTime = "6am").validate().any { it.contains("HH:mm") })
    }

    // ---- logging progress ----

    @Test
    fun progressToday_isValid() {
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        assertTrue(LogProgressRequest(today, 5.0).validate().isEmpty())
    }

    @Test
    fun malformedDate_isRejected() {
        assertTrue(LogProgressRequest("04/11/2026", 5.0).validate().any { it.contains("yyyy-MM-dd") })
    }

    @Test
    fun futureDate_isRejected() {
        val future = LocalDate.now(ZoneOffset.UTC).plusDays(5).toString()
        assertTrue(LogProgressRequest(future, 5.0).validate().any { it.contains("future") })
    }

    @Test
    fun negativeProgress_isRejected() {
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        assertTrue(LogProgressRequest(today, -1.0).validate().any { it.contains("negative") })
    }
}