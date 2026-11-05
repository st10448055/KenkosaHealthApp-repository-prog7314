package com.kenkosa.backend.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationValidationTest {

    private val goodToken = "dXkq3Zp8T0e:APA91bH_" + "x".repeat(60) + "-Q9"

    @Test
    fun validToken_isAccepted() {
        assertTrue(RegisterDeviceRequest(goodToken).validate().isEmpty())
        assertTrue(UnregisterDeviceRequest(goodToken).validate().isEmpty())
    }

    @Test
    fun shortOrBlankToken_isRejected() {
        assertEquals(1, RegisterDeviceRequest("").validate().size)
        assertEquals(1, RegisterDeviceRequest("abc").validate().size)
    }

    @Test
    fun tokenWithSlash_isRejected() {
        assertEquals(1, RegisterDeviceRequest("a".repeat(30) + "/" + "b".repeat(30)).validate().size)
    }

    @Test
    fun onlyAndroidPlatformIsSupported() {
        assertEquals(1, RegisterDeviceRequest(goodToken, platform = "ios").validate().size)
    }

    @Test
    fun announcement_needsEnglishText() {
        val request = AnnouncementRequest(title = mapOf("af" to "Hallo"), body = mapOf("af" to "Wêreld"))
        assertEquals(2, request.validate().size)
    }

    @Test
    fun announcement_rejectsUnknownLanguageAndScreen() {
        val request = AnnouncementRequest(
            title = mapOf("en" to "Hi", "zu" to "Sawubona"),
            body = mapOf("en" to "There"),
            screen = "nowhere"
        )
        assertEquals(2, request.validate().size)
    }

    @Test
    fun announcement_validExample_passes() {
        val request = AnnouncementRequest(
            title = mapOf("en" to "New feature", "af" to "Nuwe funksie", "xh" to "Isici esitsha"),
            body = mapOf("en" to "Try the new sleep chart."),
            screen = NotificationScreens.SLEEP
        )
        assertTrue(request.validate().isEmpty())
    }

    @Test
    fun languageNormalise_fallsBackToEnglish() {
        assertEquals("en", NotificationLanguages.normalise(null))
        assertEquals("en", NotificationLanguages.normalise("fr"))
        assertEquals("af", NotificationLanguages.normalise("AF"))
        assertEquals("xh", NotificationLanguages.normalise("xh"))
    }
}