package com.example.kenkosahealthapp_prog7314.ui.medication

import android.content.Context
import android.text.format.DateFormat
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

/**
 * Pure helpers that answer "is this medication due on day X?" and "was it taken?".
 * All dates are the phone's local dates, sent to / stored on the server as "yyyy-MM-dd".
 */
object MedicationSchedule {

    const val CATEGORY_MEDICATION = "MEDICATION"
    const val CATEGORY_VITAMIN = "VITAMIN"

    const val FREQUENCY_DAILY = "DAILY"
    const val FREQUENCY_WEEKLY = "WEEKLY"
    const val FREQUENCY_MONTHLY = "MONTHLY"

    /** Daily = every day, Weekly = same weekday as the start date, Monthly = same day of month. */
    fun isDueOn(med: MedicationDto, date: LocalDate): Boolean {
        val start = runCatching { LocalDate.parse(med.startDate) }.getOrNull() ?: return true
        if (date.isBefore(start)) return false
        return when (med.frequency) {
            FREQUENCY_WEEKLY -> date.dayOfWeek == start.dayOfWeek
            // Clamp so a dose started on the 31st is still due on the last day of shorter months.
            FREQUENCY_MONTHLY -> date.dayOfMonth == minOf(start.dayOfMonth, date.lengthOfMonth())
            else -> true
        }
    }

    fun isTaken(med: MedicationDto, date: LocalDate): Boolean =
        med.takenDates.contains(date.toString())

    /** Doses that were due on one of the [windowDays] days before [today] but never marked as taken. */
    fun missedDoses(meds: List<MedicationDto>, today: LocalDate, windowDays: Int = 7): Int {
        var missed = 0
        for (med in meds) {
            for (offset in 1..windowDays) {
                val day = today.minusDays(offset.toLong())
                if (isDueOn(med, day) && !isTaken(med, day)) missed++
            }
        }
        return missed
    }

    // ------------------------------------------------------------- time helpers

    /** "08:00" -> (8, 0). Falls back to 08:00 if the string is malformed. */
    fun parseTime(hhmm: String): Pair<Int, Int> {
        val parts = hhmm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        return hour to minute
    }

    fun toHHmm(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    /** Formats "HH:mm" using the phone's 12h / 24h preference. */
    fun formatTime(context: Context, hhmm: String): String {
        val (hour, minute) = parseTime(hhmm)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }
}
