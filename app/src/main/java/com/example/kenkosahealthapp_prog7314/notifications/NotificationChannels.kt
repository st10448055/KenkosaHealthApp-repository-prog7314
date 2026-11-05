package com.example.kenkosahealthapp_prog7314.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.kenkosahealthapp_prog7314.R

object NotificationChannels {

    const val UPDATES = "kenkosa_updates"
    const val ALERTS = "kenkosa_alerts"

    // Server-side names ("updates" / "alerts") -> channel ids. Anything unknown uses UPDATES.
    fun idFor(serverChannel: String?): String =
        if (serverChannel.equals("alerts", ignoreCase = true)) ALERTS else UPDATES

    // Safe to call on every launch: existing channels are left alone (names are refreshed).
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val updates = NotificationChannel(
            UPDATES,
            context.getString(R.string.notif_channel_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notif_channel_updates_desc) }

        val alerts = NotificationChannel(
            ALERTS,
            context.getString(R.string.notif_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notif_channel_alerts_desc) }

        manager.createNotificationChannels(listOf(updates, alerts))
    }
}