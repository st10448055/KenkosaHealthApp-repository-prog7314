package com.example.kenkosahealthapp_prog7314

import android.app.Application
import com.example.kenkosahealthapp_prog7314.data.sync.NetworkMonitor
import com.example.kenkosahealthapp_prog7314.data.sync.SyncScheduler
import com.example.kenkosahealthapp_prog7314.notifications.FcmTokenManager
import com.example.kenkosahealthapp_prog7314.notifications.NotificationChannels
import com.google.firebase.auth.FirebaseAuth

// Application class: runs once when the app process starts.
// Starts connectivity monitoring and schedules background sync.
class KenkosaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Watches the network and triggers a sync when it comes back.
        NetworkMonitor.start(this)

        // Safety net: syncs periodically whenever the device is online.
        SyncScheduler.schedulePeriodic(this)

        // Push notifications: create the channels, then register this phone whenever someone signs in.
        NotificationChannels.create(this)
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) FcmTokenManager.syncInBackground(this)
        }
    }

    companion object {
        // Gives repositories access to the application context.
        lateinit var instance: KenkosaApp
            private set
    }
}
