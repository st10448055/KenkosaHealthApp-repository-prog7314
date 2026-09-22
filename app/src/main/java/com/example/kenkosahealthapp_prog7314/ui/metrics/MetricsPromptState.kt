package com.example.kenkosahealthapp_prog7314.ui.metrics

// Keeps track of whether the current user skipped the metrics prompt.
object MetricsPromptState {

    // Stores the user ID so the skip applies only to that account
    @Volatile
    var skippedForUid: String? = null
}
//---------------------------------------END OF FILE-------------------------------------------------