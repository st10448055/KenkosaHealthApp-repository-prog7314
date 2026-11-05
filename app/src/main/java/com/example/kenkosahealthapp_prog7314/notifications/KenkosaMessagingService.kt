package com.example.kenkosahealthapp_prog7314.notifications

import android.util.Log
import com.example.kenkosahealthapp_prog7314.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Receives push messages from Firebase Cloud Messaging, even when the app is closed.
class KenkosaMessagingService : FirebaseMessagingService() {

    // Firebase gives the phone a new token on first install and whenever it rotates the old one.
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")
        FcmTokenManager.syncInBackground(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The user turned push notifications off inside the app: show nothing.
        if (!NotificationPreferences(applicationContext).enabled) {
            Log.d(TAG, "Message ignored: notifications are switched off in Settings")
            return
        }

        val data = message.data
        val title = data["title"] ?: message.notification?.title
        val body = data["body"] ?: message.notification?.body
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Log.d(TAG, "Message ignored: no title or body")
            return
        }

        NotificationChannels.create(applicationContext)

        NotificationHelper.show(
            context = applicationContext,
            title = title ?: getString(R.string.app_name),
            body = body.orEmpty(),
            screenKey = data["screen"],
            serverChannel = data["channel"] ?: message.notification?.channelId,
            type = data["type"]
        )
    }

    private companion object {
        const val TAG = "KenkosaFcm"
    }
}