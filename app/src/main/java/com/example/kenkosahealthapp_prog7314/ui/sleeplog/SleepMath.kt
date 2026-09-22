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

// Represents the different sleep quality levels used by the app.
enum class SleepQuality { EXCELLENT, GOOD, FAIR, POOR }

// Contains the calculations and formatting logic used for sleep data.
// Keeping this logic in one place means it does not need to be repeated
// across different screens.
object SleepMath {

    // The target amount of sleep is 8 hours, converted into minutes.
    // Other calculations use this value when measuring progress towards the goal.
    const val TARGET_MINUTES = 8 * 60

    // Uses the device's current time zone when converting timestamps into dates.
    private val zone: ZoneId get() = ZoneId.systemDefault()

    // A sleep record is assigned to the date that the user woke up.
    // The sleepEnd value is stored as milliseconds, so it is first converted
    // into an Instant and then into a LocalDate.
    fun wakeDate(record: SleepLogDto): LocalDate =
        Instant.ofEpochMilli(record.sleepEnd).atZone(zone).toLocalDate()

    // Finds the Monday of the week that the given date belongs to.
    // If the date is already Monday, that same date is returned.
    fun weekStart(day: LocalDate): LocalDate =
        day.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )

    // Calculates the total number of minutes slept on a particular day.
    // It includes all sleep records that ended on that day, including naps.
    fun minutesOn(
        records: List<SleepLogDto>,
        day: LocalDate
    ): Int =
        records
            .filter { wakeDate(it) == day }
            .sumOf { it.totalDurationMinutes }

    // Finds the longest sleep record that ended on the selected day.
    // This is used as the main sleep record when displaying the quality score.
    fun mainRecordOn(
        records: List<SleepLogDto>,
        day: LocalDate
    ): SleepLogDto? =
        records
            .filter { wakeDate(it) == day }
            .maxByOrNull { it.totalDurationMinutes }

    // Calculates a sleep score between 0 and 100.
    // 60% of the score comes from the user's quality rating,
    // while 40% comes from how close the sleep duration was to the 8-hour target.
    fun score(record: SleepLogDto): Int {

        // Converts the user's 1-5 rating into a percentage.
        // coerceIn makes sure the rating stays between 1 and 5.
        val rated = record.qualityRating.coerceIn(1, 5) * 20

        // Calculates how close the sleep duration is to the target.
        // The result is capped at 100 so sleeping more than 8 hours
        // does not increase the duration part of the score above 100.
        val duration =
            min(
                100.0,
                record.totalDurationMinutes * 100.0 / TARGET_MINUTES
            )

        // Combines the rating and duration using their respective weights.
        // roundToInt converts the final decimal value into a whole number.
        // coerceIn makes sure the final score stays between 0 and 100.
        return (0.6 * rated + 0.4 * duration)
            .roundToInt()
            .coerceIn(0, 100)
    }

    // Converts a numerical sleep score into one of the four quality categories.
    fun qualityOf(score: Int): SleepQuality = when {
        score >= 80 -> SleepQuality.EXCELLENT
        score >= 60 -> SleepQuality.GOOD
        score >= 40 -> SleepQuality.FAIR
        else -> SleepQuality.POOR
    }

    // Maps each SleepQuality value to the corresponding string resource.
    // This allows the UI to display the appropriate quality label.
    fun qualityLabelRes(quality: SleepQuality): Int = when (quality) {
        SleepQuality.EXCELLENT -> R.string.sleeplog_quality_excellent
        SleepQuality.GOOD -> R.string.sleeplog_quality_good
        SleepQuality.FAIR -> R.string.sleeplog_quality_fair
        SleepQuality.POOR -> R.string.sleeplog_quality_poor
    }

    // Maps the user's 1-5 sleep rating to the matching drawable image.
    // If the rating is outside the expected range, the highest rating image is used.
    fun faceRes(rating: Int): Int = when (rating) {
        1 -> R.drawable.ic_sleepq_1
        2 -> R.drawable.ic_sleepq_2
        3 -> R.drawable.ic_sleepq_3
        4 -> R.drawable.ic_sleepq_4
        else -> R.drawable.ic_sleepq_5
    }

    // Formats the duration into a short format such as "7h 32m".
    // This version is mainly used where space is limited, such as the sleep ring.
    fun formatShort(context: Context, minutes: Int): String =
        context.getString(
            R.string.sleeplog_duration_short,
            minutes / 60,
            minutes % 60
        )

    // Formats the duration into a longer format such as "8h 00min".
    // This is used on screens where there is more space available.
    fun formatLong(context: Context, minutes: Int): String =
        context.getString(
            R.string.sleeplog_duration_long,
            minutes / 60,
            minutes % 60
        )
}
