package com.example.kenkosahealthapp_prog7314.security

import android.content.Context
import android.util.Log

// Stores the biometric-login choice ON THE DEVICE (not in Firestore) because
// fingerprints/faces are enrolled per device, so the setting is per device too.
// The flag is stored per Firebase uid so two accounts on one phone do not affect each other.
class BiometricPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // Is biometric login switched on for this user on this device?
    fun isEnabled(uid: String): Boolean = prefs.getBoolean(KEY_ENABLED + uid, false)

    fun setEnabled(uid: String, enabled: Boolean) {
        Log.d(TAG, "Biometric login enabled=$enabled")
        prefs.edit().putBoolean(KEY_ENABLED + uid, enabled).apply()
    }

    // Has the one-time "enable biometric login?" offer already been shown?
    fun wasOfferShown(uid: String): Boolean = prefs.getBoolean(KEY_OFFER + uid, false)

    fun setOfferShown(uid: String) {
        prefs.edit().putBoolean(KEY_OFFER + uid, true).apply()
    }

    // Time the app last went to the background (0 = never). Used for the re-lock timeout.
    var backgroundedAtMs: Long
        get() = prefs.getLong(KEY_BACKGROUNDED, 0L)
        set(value) { prefs.edit().putLong(KEY_BACKGROUNDED, value).apply() }

    companion object {
        private const val TAG = "BiometricPrefs"
        private const val FILE_NAME = "kenkosa_biometric"
        private const val KEY_ENABLED = "enabled_"
        private const val KEY_OFFER = "offer_shown_"
        private const val KEY_BACKGROUNDED = "backgrounded_at"
    }
}
