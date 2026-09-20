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

enum class DayState { MET, NOT_MET, FUTURE }

data class StreakDay(val date: LocalDate, val state: DayState, val isToday: Boolean)

/**
 * All the progress and streak maths in one place, with no Android in it.
 *
 * - Every goal keeps a log of "date -> total for that day".
 * - A goal's progress bar is its total for the current period: today (daily), this Mon-Sun week (weekly) or this
 *   calendar month (monthly).
 * - A day counts towards the streak when every DAILY goal that existed that day reached its target that day.
 *   Days before you had a daily goal, or with no daily goals at all, don't count.
 */
object GoalProgress {

    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"

    private const val MAX_STREAK_DAYS = 366

    private fun log(goal: TrackedGoalDto): Map<String, Double> = goal.progressLog ?: emptyMap()

    private fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()

    fun createdDate(goal: TrackedGoalDto): LocalDate =
        Instant.ofEpochMilli(goal.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    /** What was logged on one specific day (0 if nothing). */
    fun valueOn(goal: TrackedGoalDto, day: LocalDate): Double = log(goal)[day.toString()] ?: 0.0

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Progress in the goal's current period, which is what the bar and "6 / 8" show. */
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

    fun percent(goal: TrackedGoalDto, today: LocalDate): Int {
        if (goal.targetValue <= 0) return 0
        return ((periodValue(goal, today) / goal.targetValue) * 100).toInt().coerceIn(0, 100)
    }

    private fun isMetOn(goal: TrackedGoalDto, day: LocalDate): Boolean =
        goal.targetValue > 0 && valueOn(goal, day) >= goal.targetValue

    private fun dailyGoalsOn(goals: List<TrackedGoalDto>, day: LocalDate): List<TrackedGoalDto> =
        goals.filter { it.frequency == DAILY && !createdDate(it).isAfter(day) }

    fun hasDailyGoals(goals: List<TrackedGoalDto>): Boolean = goals.any { it.frequency == DAILY }

    private fun dayMet(goals: List<TrackedGoalDto>, day: LocalDate): Boolean {
        val daily = dailyGoalsOn(goals, day)
        return daily.isNotEmpty() && daily.all { isMetOn(it, day) }
    }

    /** The seven days of this week, Monday first, for the streak row. */
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

    /**
     * Consecutive days, counting back from today, on which every daily goal was met. If today isn't done yet the
     * count starts from yesterday, so the streak isn't shown as broken until the day has actually passed.
     */
    fun currentStreak(goals: List<TrackedGoalDto>, today: LocalDate): Int {
        var day = if (dayMet(goals, today)) today else today.minusDays(1)
        var count = 0
        while (count < MAX_STREAK_DAYS && dayMet(goals, day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** 8400.0 -> "8.4k", 10000.0 -> "10k", 7.2 -> "7.2", 6.0 -> "6". */
    fun formatCompact(value: Double): String {
        fun trimmed(v: Double) = String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
        return if (abs(value) >= 1000) trimmed(value / 1000) + "k" else trimmed(value)
    }
}
