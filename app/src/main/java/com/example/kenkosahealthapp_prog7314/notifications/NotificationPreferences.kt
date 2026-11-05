package com.example.kenkosahealthapp_prog7314.notifications

import android.content.Context
import androidx.core.content.edit

// On-device store for notification state (SharedPreferences, so it works offline).
class NotificationPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Mirror of the "Push notifications" switch in Settings. On by default.
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    // True once the system permission prompt has been shown by us.
    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED, value) }

    // "uid|token" last confirmed saved on the server.
    var registeredKey: String?
        get() = prefs.getString(KEY_REGISTERED, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_REGISTERED) else putString(KEY_REGISTERED, value) }

    // The announcements topic this phone is subscribed to (e.g. "announcements_af").
    var topic: String?
        get() = prefs.getString(KEY_TOPIC, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_TOPIC) else putString(KEY_TOPIC, value) }

    private companion object {
        const val FILE = "kenkosa_notifications"
        const val KEY_ENABLED = "enabled"
        const val KEY_ASKED = "permission_asked"
        const val KEY_REGISTERED = "registered_key"
        const val KEY_TOPIC = "topic"
    }
}