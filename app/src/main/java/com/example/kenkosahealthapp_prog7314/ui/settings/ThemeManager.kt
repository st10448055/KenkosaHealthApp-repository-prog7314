package com.example.kenkosahealthapp_prog7314.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

// Applies the user's Light / Dark choice.
//
// Light is the default. Anything that isn't "Dark" (including a legacy
// "System" value) is treated as Light.
//
// The choice is also cached in SharedPreferences so it can be applied in
// MainActivity.onCreate before any UI is inflated (Firestore is async, so
// waiting for it would flash the wrong theme on launch).
object ThemeManager {

    private const val PREFS = "kenkosa_prefs"
    private const val KEY_APPEARANCE = "appearance_mode"
    private const val DEFAULT_MODE = "Light"

    // Reads the cached theme choice from SharedPreferences and applies it immediately.
    // Used at app startup, before Firestore settings would otherwise be available.
    fun applySaved(context: Context) {
        val mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_APPEARANCE, DEFAULT_MODE) ?: DEFAULT_MODE
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    // Saves mode ("Light" | "Dark") to SharedPreferences and applies it
    // (this recreates the activity if the mode actually changed).
    fun set(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APPEARANCE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    // Maps our stored mode string to the AppCompatDelegate night mode constant
    private fun toNightMode(mode: String): Int = when (mode) {
        "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_NO // "Light", "System", or anything unknown
    }
}