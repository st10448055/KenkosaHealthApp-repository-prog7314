package com.example.kenkosahealthapp_prog7314.notifications

// The screens a notification can open. The server sends the lowercase key in the "screen" field.
enum class NotificationScreen(val key: String) {
    HOME("home"),
    WATER("water"),
    GOALS("goals"),
    SLEEP("sleep"),
    FASTING("fasting"),
    MEDICATION("medication"),
    WELLNESS("wellness"),
    SETTINGS("settings");

    companion object {
        // Unknown or missing keys return null, so a bad message simply opens the app normally.
        fun fromKey(key: String?): NotificationScreen? =
            values().firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) }
    }
}