package com.example.kenkosahealthapp_prog7314.notifications

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.kenkosahealthapp_prog7314.data.network.RegisterDeviceRequest
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UnregisterDeviceRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

// Keeps this phone's FCM token registered with the backend.
// If registration fails offline, SyncWorker calls syncPending() again later.
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val LOGOUT_TIMEOUT_MS = 3_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fire-and-forget version for callers that cannot suspend.
    fun syncInBackground(context: Context) {
        val appContext = context.applicationContext
        scope.launch { syncPending(appContext) }
    }

    // Registers the token if someone is signed in, notifications are on, and this
    // user+token pair is not already saved. Returns false if it should be retried.
    suspend fun syncPending(context: Context): Boolean {
        val appContext = context.applicationContext
        val prefs = NotificationPreferences(appContext)
        val user = FirebaseAuth.getInstance().currentUser ?: return true
        if (!prefs.enabled) return true

        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            val key = "${user.uid}|$token"

            if (prefs.registeredKey != key) {
                val response = RetrofitInstance.deviceApi.registerToken(RegisterDeviceRequest(token))
                if (!response.isSuccessful) {
                    Log.w(TAG, "Token registration rejected: HTTP ${response.code()}")
                    return !isTemporary(response.code())
                }
                prefs.registeredKey = key
                Log.d(TAG, "Token registered with the server")
            }
            syncTopic(prefs)
            true
        } catch (e: IOException) {
            Log.d(TAG, "Offline, will register the token later")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Token registration failed", e)
            false
        }
    }

    // Called from the Settings switch. ON registers the phone; OFF removes it.
    fun onPreferenceChanged(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        NotificationPreferences(appContext).enabled = enabled
        scope.launch {
            if (enabled) {
                syncPending(appContext)
            } else {
                runCatching { unregisterFromServer(appContext) }
                runCatching { unsubscribeTopic(NotificationPreferences(appContext)) }
            }
        }
    }

    // Announcements follow the app language.
    fun onLanguageChanged(context: Context) {
        val appContext = context.applicationContext
        scope.launch { runCatching { syncTopic(NotificationPreferences(appContext)) } }
    }

    // Must run BEFORE FirebaseAuth.signOut(): the server call needs the login token.
    // Short timeout so signing out is never held up by a bad connection.
    suspend fun detachDevice(context: Context) {
        val appContext = context.applicationContext
        withTimeoutOrNull(LOGOUT_TIMEOUT_MS) {
            runCatching { unregisterFromServer(appContext) }
            runCatching { unsubscribeTopic(NotificationPreferences(appContext)) }
            // Kills this phone's token, so it can never deliver the previous user's pushes.
            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
        }
        NotificationPreferences(appContext).registeredKey = null
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun unregisterFromServer(context: Context) {
        val token = FirebaseMessaging.getInstance().token.await()
        RetrofitInstance.deviceApi.unregisterToken(UnregisterDeviceRequest(token))
        NotificationPreferences(context).registeredKey = null
    }

    // Subscribes to "announcements_<language>" and leaves any other language's topic.
    private suspend fun syncTopic(prefs: NotificationPreferences) {
        val wanted = "announcements_${currentLanguage()}"
        if (prefs.topic == wanted) return
        val messaging = FirebaseMessaging.getInstance()
        prefs.topic?.let { messaging.unsubscribeFromTopic(it).await() }
        messaging.subscribeToTopic(wanted).await()
        prefs.topic = wanted
    }

    private suspend fun unsubscribeTopic(prefs: NotificationPreferences) {
        val current = prefs.topic ?: return
        FirebaseMessaging.getInstance().unsubscribeFromTopic(current).await()
        prefs.topic = null
    }

    // Read on the main thread because AppCompat expects that.
    private suspend fun currentLanguage(): String = withContext(Dispatchers.Main.immediate) {
        AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?.takeIf { it == "af" || it == "xh" }
            ?: "en"
    }

    private fun isTemporary(code: Int): Boolean =
        code >= 500 || code in listOf(401, 403, 408, 429)
}