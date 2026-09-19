package com.example.kenkosahealthapp_prog7314.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Applies the user's Light / Dark / System choice.
 *
 * The choice is also cached in SharedPreferences so it can be applied in
 * MainActivity.onCreate before any UI is inflated (Firestore is async, so
 * waiting for it would flash the wrong theme on launch).
 */
object ThemeManager {

    private const val PREFS = "kenkosa_prefs"
    private const val KEY_APPEARANCE = "appearance_mode"

    fun applySaved(context: Context) {
        val mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_APPEARANCE, "System") ?: "System"
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    /** Saves [mode] ("Light" | "Dark" | "System") and applies it (recreates the activity if it changed). */
    fun set(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APPEARANCE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    private fun toNightMode(mode: String): Int = when (mode) {
        "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "Light" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
