package com.example.kenkosahealthapp_prog7314.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.MainActivity
import com.example.kenkosahealthapp_prog7314.R

object NotificationHelper {

    // Key of the extra that tells MainActivity which screen to open.
    const val EXTRA_SCREEN = "kenkosa.notification.screen"

    fun show(
        context: Context,
        title: String,
        body: String,
        screenKey: String?,
        serverChannel: String?,
        type: String?
    ) {
        // Android 13+ needs the runtime permission.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        // Same id for the same kind of notification, so a newer one replaces the older one.
        val notificationId = (type ?: title).hashCode()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            screenKey?.let { putExtra(EXTRA_SCREEN, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.idFor(serverChannel))
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(ContextCompat.getColor(context, R.color.kenkosa_green_dark))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}