package com.kenkosa.backend.notifications

import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.kenkosa.backend.models.NotificationLanguages
import com.kenkosa.backend.repository.DeviceTokenRepository
import com.kenkosa.backend.repository.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration

// Sends "data" messages: the server sends the words and destination, the app builds the notification.
class NotificationService(private val devices: DeviceTokenRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fire-and-forget: a failed push must never affect the user's data.
    fun sendToUserAsync(userId: String, type: NotificationType, detail: String? = null) {
        scope.launch {
            try {
                sendToUser(userId, type, detail)
            } catch (e: Exception) {
                println("FCM: could not send $type to $userId: ${e.message}")
            }
        }
    }

    suspend fun sendToUser(userId: String, type: NotificationType, detail: String? = null): SendOutcome {
        val settings = readSettings(userId)
        if (!settings.notificationsEnabled) return SendOutcome(devices = 0, sent = 0)

        val tokens = devices.tokensFor(userId)
        if (tokens.isEmpty()) return SendOutcome(devices = 0, sent = 0)

        val text = NotificationTexts.forType(type, settings.language, detail)
        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .putData("type", type.name)
            .putData("channel", type.channel)
            .putData("title", text.title)
            .putData("body", text.body)
            .putData("screen", type.screen)
            .setAndroidConfig(androidConfig(collapseKey = type.name))
            .build()

        val response = FirebaseMessaging.getInstance().sendEachForMulticastAsync(message).await()

        // Remove tokens that no longer work.
        val dead = response.responses.mapIndexedNotNull { index, result ->
            val code = result.exception?.messagingErrorCode
            if (!result.isSuccessful && (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH)) {
                tokens[index]
            } else null
        }
        if (dead.isNotEmpty()) devices.removeAll(dead)

        return SendOutcome(devices = tokens.size, sent = response.successCount)
    }

    // Phones subscribe to announcements_en / announcements_af / announcements_xh.
    suspend fun broadcast(
        titles: Map<String, String>,
        bodies: Map<String, String>,
        screen: String?
    ): Map<String, String> {
        val sent = linkedMapOf<String, String>()
        for (lang in NotificationLanguages.SUPPORTED) {
            val title = titles[lang] ?: titles["en"] ?: continue
            val body = bodies[lang] ?: bodies["en"] ?: continue
            val message = Message.builder()
                .setTopic("announcements_$lang")
                .putData("type", NotificationType.ANNOUNCEMENT.name)
                .putData("channel", NotificationType.ANNOUNCEMENT.channel)
                .putData("title", title)
                .putData("body", body)
                .putData("screen", screen ?: NotificationType.ANNOUNCEMENT.screen)
                .setAndroidConfig(androidConfig(collapseKey = "ANNOUNCEMENT", ttl = Duration.ofDays(1)))
                .build()
            sent[lang] = FirebaseMessaging.getInstance().sendAsync(message).await()
        }
        return sent
    }

    private fun androidConfig(collapseKey: String, ttl: Duration = Duration.ofHours(6)): AndroidConfig =
        AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setTtl(ttl.toMillis())
            .setCollapseKey(collapseKey)
            .build()

    // Reads the same Firestore document the app writes. Missing values = notifications on, English.
    private suspend fun readSettings(userId: String): UserNotificationSettings {
        val doc = FirestoreClient.getFirestore().collection("userSettings").document(userId).get().await()
        return UserNotificationSettings(
            notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true,
            language = NotificationLanguages.normalise(doc.getString("language"))
        )
    }

    private data class UserNotificationSettings(val notificationsEnabled: Boolean, val language: String)
}

data class SendOutcome(val devices: Int, val sent: Int)