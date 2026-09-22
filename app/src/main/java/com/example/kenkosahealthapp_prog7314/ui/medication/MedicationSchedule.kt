package com.example.kenkosahealthapp_prog7314.ui.medication

import android.content.Context
import android.text.format.DateFormat
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

// Helper object containing the rules used to determine medication schedules.
// These functions do not directly modify the database or UI.
object MedicationSchedule {

    // Category value used for normal medication.
    const val CATEGORY_MEDICATION = "MEDICATION"

    // Category value used for vitamins.
    const val CATEGORY_VITAMIN = "VITAMIN"

    // Available medication frequency values.
    const val FREQUENCY_DAILY = "DAILY"
    const val FREQUENCY_WEEKLY = "WEEKLY"
    const val FREQUENCY_MONTHLY = "MONTHLY"

    // Determines whether a medication should be taken on a specific date.
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

    // Checks whether the medication was marked as taken on the specified date.
    fun isTaken(med: MedicationDto, date: LocalDate): Boolean =
        med.takenDates.contains(date.toString())

    // Counts medication doses that were due in the previous windowDays
    // but were never marked as taken.
    fun missedDoses(meds: List<MedicationDto>, today: LocalDate, windowDays: Int = 7): Int {
        // Start the missed-dose counter at zero.
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

    // Converts a time string such as "08:00" into an hour and minute pair.
    fun parseTime(hhmm: String): Pair<Int, Int> {
        val parts = hhmm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: 8

        // Get the minute and make sure it is between 0 and 59.
        // If invalid, use 0.
        val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        return hour to minute
    }

    fun toHHmm(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    // Converts a "HH:mm" time into the user's preferred 12-hour or 24-hour format.
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
//---------------------------------------END OF FILE-------------------------------------------------