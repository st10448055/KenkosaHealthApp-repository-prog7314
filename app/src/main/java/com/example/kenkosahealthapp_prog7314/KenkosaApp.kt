package com.example.kenkosahealthapp_prog7314

import android.app.Application
import com.example.kenkosahealthapp_prog7314.data.sync.NetworkMonitor
import com.example.kenkosahealthapp_prog7314.data.sync.SyncScheduler

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
    }

    companion object {
        // Gives repositories access to the application context.
        lateinit var instance: KenkosaApp
            private set
    }
}
