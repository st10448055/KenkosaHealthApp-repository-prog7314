package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.content.Context
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.min
import kotlin.math.roundToInt

enum class SleepQuality { EXCELLENT, GOOD, FAIR, POOR }

/** Sleep maths in one place, with no Android views in it. */
object SleepMath {

    /** The nightly goal the ring fills towards. Change it here. */
    const val TARGET_MINUTES = 8 * 60

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** A night belongs to the day you woke up on. */
    fun wakeDate(record: SleepLogDto): LocalDate =
        Instant.ofEpochMilli(record.sleepEnd).atZone(zone).toLocalDate()

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Total minutes slept on [day] (all the sleeps that ended that day, naps included). */
    fun minutesOn(records: List<SleepLogDto>, day: LocalDate): Int =
        records.filter { wakeDate(it) == day }.sumOf { it.totalDurationMinutes }

    /** The longest sleep that ended on [day]; its quality is what the score card shows. */
    fun mainRecordOn(records: List<SleepLogDto>, day: LocalDate): SleepLogDto? =
        records.filter { wakeDate(it) == day }.maxByOrNull { it.totalDurationMinutes }

    /** 0-100: 60% how you rated it, 40% how close the night got to the target. */
    fun score(record: SleepLogDto): Int {
        val rated = record.qualityRating.coerceIn(1, 5) * 20
        val duration = min(100.0, record.totalDurationMinutes * 100.0 / TARGET_MINUTES)
        return (0.6 * rated + 0.4 * duration).roundToInt().coerceIn(0, 100)
    }

    fun qualityOf(score: Int): SleepQuality = when {
        score >= 80 -> SleepQuality.EXCELLENT
        score >= 60 -> SleepQuality.GOOD
        score >= 40 -> SleepQuality.FAIR
        else -> SleepQuality.POOR
    }

    fun qualityLabelRes(quality: SleepQuality): Int = when (quality) {
        SleepQuality.EXCELLENT -> R.string.sleeplog_quality_excellent
        SleepQuality.GOOD -> R.string.sleeplog_quality_good
        SleepQuality.FAIR -> R.string.sleeplog_quality_fair
        SleepQuality.POOR -> R.string.sleeplog_quality_poor
    }

    fun faceRes(rating: Int): Int = when (rating) {
        1 -> R.drawable.ic_sleepq_1
        2 -> R.drawable.ic_sleepq_2
        3 -> R.drawable.ic_sleepq_3
        4 -> R.drawable.ic_sleepq_4
        else -> R.drawable.ic_sleepq_5
    }

    /** 452 -> "7h 32m" (the ring). */
    fun formatShort(context: Context, minutes: Int): String =
        context.getString(R.string.sleeplog_duration_short, minutes / 60, minutes % 60)

    /** 480 -> "8h 00min" (the Add Sleep screen and the list). */
    fun formatLong(context: Context, minutes: Int): String =
        context.getString(R.string.sleeplog_duration_long, minutes / 60, minutes % 60)
}
