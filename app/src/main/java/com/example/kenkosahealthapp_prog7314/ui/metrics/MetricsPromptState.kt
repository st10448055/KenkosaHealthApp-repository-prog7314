package com.example.kenkosahealthapp_prog7314.ui.metrics

/**
 * Remembers that this user chose "Skip for now", so Home doesn't ask again until the app is restarted.
 * Tied to the user id so a different account signing in on the same device is still prompted.
 */
object MetricsPromptState {
    @Volatile
    var skippedForUid: String? = null
}
