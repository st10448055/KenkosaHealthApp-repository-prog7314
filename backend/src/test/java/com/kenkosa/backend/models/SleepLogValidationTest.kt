package com.kenkosa.backend.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SleepLogValidationTest {

    private val hour = 3_600_000L
    private fun now() = System.currentTimeMillis()

    @Test
    fun validSleep_hasNoErrors() {
        val end = now() - 60_000
        assertTrue(SaveSleepLogRequest(end - 8 * hour, end, 4).validate().isEmpty())
    }

    @Test
    fun wakeBeforeBedtime_isRejected() {
        val t = now() - hour
        val errors = SaveSleepLogRequest(sleepStart = t, sleepEnd = t - 1000, qualityRating = 3).validate()
        assertTrue(errors.any { it.contains("after bedtime") })
    }

    @Test
    fun sleepLongerThan24Hours_isRejected() {
        val end = now() - 60_000
        val errors = SaveSleepLogRequest(end - 25 * hour, end, 3).validate()
        assertTrue(errors.any { it.contains("24 hours") })
    }

    @Test
    fun futureWakeTime_isRejected() {
        val errors = SaveSleepLogRequest(now() + hour, now() + 9 * hour, 3).validate()
        assertTrue(errors.any { it.contains("future") })
    }

    @Test
    fun ratingOutsideOneToFive_isRejected() {
        val end = now() - 60_000
        assertTrue(SaveSleepLogRequest(end - 8 * hour, end, 0).validate().isNotEmpty())
        assertTrue(SaveSleepLogRequest(end - 8 * hour, end, 6).validate().isNotEmpty())
    }

    @Test
    fun notesLongerThan500Characters_areRejected() {
        val end = now() - 60_000
        val errors = SaveSleepLogRequest(end - 8 * hour, end, 4, notes = "a".repeat(501)).validate()
        assertTrue(errors.any { it.contains("500") })
    }

    // ---- overlap protection ----

    @Test
    fun overlappingSleeps_areDetected() {
        // existing 22:00-06:00 vs new 05:00-09:00
        assertTrue(sleepsOverlap(22 * hour, 30 * hour, 29 * hour, 33 * hour))
    }

    @Test
    fun sleepInsideAnotherSleep_isDetected() {
        assertTrue(sleepsOverlap(22 * hour, 30 * hour, 23 * hour, 25 * hour))
    }

    @Test
    fun backToBackSleeps_doNotOverlap() {
        assertFalse(sleepsOverlap(22 * hour, 30 * hour, 30 * hour, 31 * hour))
    }

    @Test
    fun separateSleeps_doNotOverlap() {
        assertFalse(sleepsOverlap(22 * hour, 30 * hour, 46 * hour, 54 * hour))
    }
}