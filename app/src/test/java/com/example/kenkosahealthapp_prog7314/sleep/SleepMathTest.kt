package com.example.kenkosahealthapp_prog7314

import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import com.example.kenkosahealthapp_prog7314.ui.sleeplog.SleepMath
import com.example.kenkosahealthapp_prog7314.ui.sleeplog.SleepQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Unit tests for the sleep score, quality bands and per-day totals (pure maths, no Android needed). */
class SleepMathTest {

    private fun millis(day: LocalDate, hour: Int, minute: Int = 0): Long =
        day.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** A night that ENDED at 07:00 on [wakeDay] and lasted [minutes]. */
    private fun night(wakeDay: LocalDate, minutes: Int, rating: Int = 4, id: String = "n"): SleepLogDto {
        val end = millis(wakeDay, 7)
        return SleepLogDto(
            sleepId = id,
            userId = "u1",
            sleepStart = end - minutes * 60_000L,
            sleepEnd = end,
            totalDurationMinutes = minutes,
            qualityRating = rating,
            qualityScore = rating * 20,
            notes = null,
            createdAt = 0L,
            updatedAt = null
        )
    }

    // ---- score: 60% rating + 40% duration (target = 8 h) ----

    @Test
    fun score_eightHoursRatedFour_is88() {
        val day = LocalDate.of(2026, 11, 3)
        assertEquals(88, SleepMath.score(night(day, 8 * 60, rating = 4)))
    }

    @Test
    fun score_perfectNight_is100() {
        val day = LocalDate.of(2026, 11, 3)
        assertEquals(100, SleepMath.score(night(day, 8 * 60, rating = 5)))
    }

    @Test
    fun score_durationBeyondTarget_isCappedAt100() {
        val day = LocalDate.of(2026, 11, 3)
        assertEquals(100, SleepMath.score(night(day, 16 * 60, rating = 5)))
    }

    @Test
    fun score_shortPoorNight_isLow() {
        val day = LocalDate.of(2026, 11, 3)
        // 4 h rated 1: 0.6*20 + 0.4*50 = 32
        assertEquals(32, SleepMath.score(night(day, 4 * 60, rating = 1)))
    }

    @Test
    fun score_isAlwaysBetween0And100() {
        val day = LocalDate.of(2026, 11, 3)
        for (rating in 1..5) {
            for (minutes in listOf(0, 60, 240, 480, 900)) {
                val s = SleepMath.score(night(day, minutes, rating))
                assertTrue("score $s out of range", s in 0..100)
            }
        }
    }

    // ---- quality bands ----

    @Test
    fun qualityOf_highScore_isExcellent() {
        assertEquals(SleepQuality.EXCELLENT, SleepMath.qualityOf(95))
    }

    @Test
    fun qualityOf_boundaries_areCorrect() {
        assertEquals(SleepQuality.EXCELLENT, SleepMath.qualityOf(80))
        assertEquals(SleepQuality.GOOD, SleepMath.qualityOf(79))
        assertEquals(SleepQuality.GOOD, SleepMath.qualityOf(60))
        assertEquals(SleepQuality.FAIR, SleepMath.qualityOf(59))
        assertEquals(SleepQuality.FAIR, SleepMath.qualityOf(40))
        assertEquals(SleepQuality.POOR, SleepMath.qualityOf(39))
    }

    // ---- per-day grouping (a night belongs to the day you woke up) ----

    @Test
    fun minutesOn_sumsEverySleepThatEndedThatDay() {
        val day = LocalDate.of(2026, 11, 3)
        val records = listOf(
            night(day, 420, id = "main"),
            night(day, 60, id = "nap"),
            night(day.minusDays(1), 480, id = "yesterday")
        )
        assertEquals(480, SleepMath.minutesOn(records, day))
    }

    @Test
    fun minutesOn_dayWithNoSleep_isZero() {
        val records = listOf(night(LocalDate.of(2026, 11, 3), 420))
        assertEquals(0, SleepMath.minutesOn(records, LocalDate.of(2026, 11, 10)))
    }

    @Test
    fun mainRecordOn_returnsTheLongestSleep() {
        val day = LocalDate.of(2026, 11, 3)
        val records = listOf(night(day, 60, id = "nap"), night(day, 450, id = "main"))
        assertEquals("main", SleepMath.mainRecordOn(records, day)?.sleepId)
    }

    @Test
    fun mainRecordOn_noSleep_isNull() {
        assertNull(SleepMath.mainRecordOn(emptyList(), LocalDate.of(2026, 11, 3)))
    }

    // ---- weeks start on Monday ----

    @Test
    fun weekStart_wednesdayGivesThatMonday() {
        // 4 Nov 2026 is a Wednesday
        val monday = SleepMath.weekStart(LocalDate.of(2026, 11, 4))
        assertNotNull(monday)
        assertEquals(LocalDate.of(2026, 11, 2), monday)
    }

    @Test
    fun weekStart_mondayStaysTheSameDay() {
        assertEquals(LocalDate.of(2026, 11, 2), SleepMath.weekStart(LocalDate.of(2026, 11, 2)))
    }
}