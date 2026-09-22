package com.example.kenkosahealthapp_prog7314.ui.goals

import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

// Status of a specific day in streak calculations
enum class DayState { MET, NOT_MET, FUTURE }

// Data holder representing a day item in weekly streak overview
data class StreakDay(val date: LocalDate, val state: DayState, val isToday: Boolean)

// Pure Kotlin helper class managing goal computations, streaks, and data formatting
object GoalProgress {

    // Goal frequency identifiers
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"

    // Maximum history limit for evaluating consecutive streaks
    private const val MAX_STREAK_DAYS = 366

    // Extracts progress log map or returns empty map if null
    private fun log(goal: TrackedGoalDto): Map<String, Double> = goal.progressLog ?: emptyMap()

    // Resolves local creation date from timestamp epoch millis
    private fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()


    fun createdDate(goal: TrackedGoalDto): LocalDate =
        Instant.ofEpochMilli(goal.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    // Returns logged progress amount for a given day (0.0 if not found)
    fun valueOn(goal: TrackedGoalDto, day: LocalDate): Double = log(goal)[day.toString()] ?: 0.0

    // Gets Monday date starting the calendar week for given date
    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Calculates current period cumulative value based on frequency (Daily, Weekly, Monthly)
    fun periodValue(goal: TrackedGoalDto, today: LocalDate): Double = when (goal.frequency) {
        WEEKLY -> {
            val start = weekStart(today)
            val end = start.plusDays(6)
            log(goal).entries.sumOf { (key, value) ->
                val date = parse(key)
                if (date != null && !date.isBefore(start) && !date.isAfter(end)) value else 0.0
            }
        }
        MONTHLY -> {
            val month = YearMonth.from(today)
            log(goal).entries.sumOf { (key, value) ->
                val date = parse(key)
                if (date != null && YearMonth.from(date) == month) value else 0.0
            }
        }
        else -> valueOn(goal, today)
    }

    // Calculates percentage completion relative to target for the current period
    fun percent(goal: TrackedGoalDto, today: LocalDate): Int {
        if (goal.targetValue <= 0) return 0
        return ((periodValue(goal, today) / goal.targetValue) * 100).toInt().coerceIn(0, 100)
    }

    // Determines if goal reached target value on given day
    private fun isMetOn(goal: TrackedGoalDto, day: LocalDate): Boolean =
        goal.targetValue > 0 && valueOn(goal, day) >= goal.targetValue

    // Returns daily goals existing on or before given date
    private fun dailyGoalsOn(goals: List<TrackedGoalDto>, day: LocalDate): List<TrackedGoalDto> =
        goals.filter { it.frequency == DAILY && !createdDate(it).isAfter(day) }

    // Checks if list contains any daily goals
    fun hasDailyGoals(goals: List<TrackedGoalDto>): Boolean = goals.any { it.frequency == DAILY }

    // Evaluates if all active daily goals reached targets on given date
    private fun dayMet(goals: List<TrackedGoalDto>, day: LocalDate): Boolean {
        val daily = dailyGoalsOn(goals, day)
        return daily.isNotEmpty() && daily.all { isMetOn(it, day) }
    }

    // Generates 7 StreakDay items representing Monday to Sunday of current week
    fun weekDays(goals: List<TrackedGoalDto>, today: LocalDate): List<StreakDay> {
        val start = weekStart(today)
        return (0..6).map { offset ->
            val day = start.plusDays(offset.toLong())
            val state = when {
                day.isAfter(today) -> DayState.FUTURE
                dayMet(goals, day) -> DayState.MET
                else -> DayState.NOT_MET
            }
            StreakDay(day, state, day == today)
        }
    }

    // Calculates current active streak working back from today (or yesterday if today isn't met)
    fun currentStreak(goals: List<TrackedGoalDto>, today: LocalDate): Int {
        var day = if (dayMet(goals, today)) today else today.minusDays(1)
        var count = 0
        while (count < MAX_STREAK_DAYS && dayMet(goals, day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    // Formats numbers concisely (e.g. 8400.0 -> "8.4k", 6.0 -> "6")
    fun formatCompact(value: Double): String {
        fun trimmed(v: Double) = String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
        return if (abs(value) >= 1000) trimmed(value / 1000) + "k" else trimmed(value)
    }
}
//---------------------------------------END OF FILE-------------------------------------------------