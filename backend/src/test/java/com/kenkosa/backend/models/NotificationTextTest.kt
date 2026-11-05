package com.kenkosa.backend.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NotificationTextTest {

    private val languages = listOf("en", "af", "xh")

    @Test
    fun everyTypeHasTextInEveryLanguage() {
        for (type in NotificationType.values()) {
            if (type == NotificationType.ANNOUNCEMENT) continue
            for (lang in languages) {
                val text = NotificationTexts.forType(type, lang, "Drink water")
                assertTrue(text.title.isNotBlank(), "$type/$lang title")
                assertTrue(text.body.isNotBlank(), "$type/$lang body")
            }
        }
    }

    @Test
    fun languagesAreActuallyDifferent() {
        val en = NotificationTexts.forType(NotificationType.TEST, "en")
        val af = NotificationTexts.forType(NotificationType.TEST, "af")
        val xh = NotificationTexts.forType(NotificationType.TEST, "xh")
        assertNotEquals(en.title, af.title)
        assertNotEquals(en.title, xh.title)
    }

    @Test
    fun unknownLanguage_fallsBackToEnglish() {
        assertEquals(
            NotificationTexts.forType(NotificationType.TEST, "en"),
            NotificationTexts.forType(NotificationType.TEST, "fr")
        )
        assertEquals(
            NotificationTexts.forType(NotificationType.TEST, "en"),
            NotificationTexts.forType(NotificationType.TEST, null)
        )
    }

    @Test
    fun goalCompleted_includesGoalName_whenGiven() {
        for (lang in languages) {
            val text = NotificationTexts.forType(NotificationType.GOAL_COMPLETED, lang, "Run 5 km")
            assertTrue("Run 5 km" in text.body, "$lang should mention the goal")
        }
    }

    @Test
    fun goalCompleted_worksWithoutGoalName() {
        for (lang in languages) {
            assertTrue(NotificationTexts.forType(NotificationType.GOAL_COMPLETED, lang, null).body.isNotBlank())
        }
    }
}