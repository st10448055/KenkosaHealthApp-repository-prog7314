package com.example.kenkosahealthapp_prog7314.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Tracks whether the device currently has internet access and
// triggers a sync the moment a connection is re-established.
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)

    // Observable online/offline state for the UI and repositories.
    val isOnline: StateFlow<Boolean> = _isOnline

    fun start(context: Context) {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(ConnectivityManager::class.java)

        // Initial state.
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                val wasOffline = !_isOnline.value
                _isOnline.value = true

                // Connection restored: upload anything made while offline.
                if (wasOffline) SyncScheduler.syncNow(appContext)
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }
}
