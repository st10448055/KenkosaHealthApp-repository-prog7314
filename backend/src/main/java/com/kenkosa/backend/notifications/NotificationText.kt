package com.kenkosa.backend.notifications

import com.kenkosa.backend.models.NotificationLanguages
import com.kenkosa.backend.models.NotificationScreens

// The kinds of push notification the server sends on its own.
enum class NotificationType(
    val channel: String,       // Android channel the app posts it in
    val screen: String         // Screen opened when the user taps it
) {
    WATER_GOAL_REACHED(channel = "alerts", screen = NotificationScreens.WATER),
    GOAL_COMPLETED(channel = "alerts", screen = NotificationScreens.GOALS),
    TEST(channel = "updates", screen = NotificationScreens.SETTINGS),
    ANNOUNCEMENT(channel = "updates", screen = NotificationScreens.HOME)
}

data class NotificationText(val title: String, val body: String)

// All user-facing wording, in English, Afrikaans and isiXhosa.
object NotificationTexts {

    fun forType(type: NotificationType, language: String?, detail: String? = null): NotificationText {
        val lang = NotificationLanguages.normalise(language)
        val name = detail?.trim().takeUnless { it.isNullOrEmpty() }
        return when (type) {
            NotificationType.WATER_GOAL_REACHED -> when (lang) {
                "af" -> NotificationText("Waterdoel bereik 💧", "Jy het jou waterdoel vandag bereik. Mooi so!")
                "xh" -> NotificationText("Ithagethi yamanzi ifezekisiwe 💧", "Ugqibile ukusela amanzi ofanele ukuwasela namhlanje. Wenze kakuhle!")
                else -> NotificationText("Hydration goal reached 💧", "You've reached your water goal for today. Well done!")
            }

            NotificationType.GOAL_COMPLETED -> when (lang) {
                "af" -> NotificationText(
                    "Doel bereik 🎉",
                    if (name != null) "Jy het jou doel bereik: $name. Puik werk!" else "Jy het jou doel bereik. Puik werk!"
                )
                "xh" -> NotificationText(
                    "Ithagethi ifezekisiwe 🎉",
                    if (name != null) "Ufikelele kwithagethi yakho: $name. Umsebenzi omhle!" else "Ufikelele kwithagethi yakho. Umsebenzi omhle!"
                )
                else -> NotificationText(
                    "Goal completed 🎉",
                    if (name != null) "You've reached your goal: $name. Great work!" else "You've reached your goal. Great work!"
                )
            }

            NotificationType.TEST -> when (lang) {
                "af" -> NotificationText("Toetskennisgewing", "Stootkennisgewings werk op hierdie toestel.")
                "xh" -> NotificationText("Isaziso sovavanyo", "Izaziso ziyasebenza kule fowuni.")
                else -> NotificationText("Test notification", "Push notifications are working on this device.")
            }

            NotificationType.ANNOUNCEMENT -> NotificationText("Kenkosa Health", detail ?: "")
        }
    }
}