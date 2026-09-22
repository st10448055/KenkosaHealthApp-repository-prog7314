package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.example.kenkosahealthapp_prog7314.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Shared helpers for formatting fast durations and timestamps consistently
// across the fasting screens (ring label, history list, add/edit screen, etc).
object FastTimeFormat {

    // 12_180_000 ms -> "3 Hours 23 Mins"; under an hour -> "23 Mins".
    fun duration(context: Context, millis: Long): String {
        // Guard against negative durations, then convert to whole minutes.
        val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val res = context.resources

        // Only include the "hours" segment if there's at least 1 hour; uses plurals
        // resources so it correctly reads "1 Hour" vs "3 Hours" etc.
        val hoursText = if (hours > 0) res.getQuantityString(R.plurals.fasting_hours, hours, hours) else null
        val minutesText = res.getQuantityString(R.plurals.fasting_minutes, minutes, minutes)
        return listOfNotNull(hoursText, minutesText).joinToString(" ")
    }

    // "Today at 5:32 AM", "Yesterday at 9:00 PM", or "Sep 18 at 6:42 PM".
    fun dayAndTime(context: Context, millis: Long): String {
        val zone = ZoneId.systemDefault()

        // Convert the raw epoch millis into a calendar date in the device's local time zone.
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)

        // Format just the time-of-day portion (respects the user's 12h/24h system setting).
        val time = DateFormat.getTimeFormat(context).format(Date(millis))

        // Pick a relative label ("Today"/"Yesterday"/"Tomorrow") when the date is close to
        // now, otherwise fall back to a full date string, and append the formatted time.
        return when (date) {
            today -> context.getString(R.string.fasting_today_at, time)
            today.minusDays(1) -> context.getString(R.string.fasting_yesterday_at, time)
            today.plusDays(1) -> context.getString(R.string.fasting_tomorrow_at, time)
            else -> context.getString(
                R.string.fasting_date_at,
                DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH),
                time
            )
        }
    }

    // "Sep 18, 5:32 AM – Sep 19, 6:42 AM"
    fun range(context: Context, startMillis: Long, endMillis: Long): String =
    // Lets the platform handle the full start–end range formatting (including
        // collapsing shared date/year info when start and end fall on the same day).
        DateUtils.formatDateRange(
            context, startMillis, endMillis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        )
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------