package com.example.kenkosahealthapp_prog7314.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationScreenTest {

    @Test
    fun knownKeys_mapToScreens() {
        assertEquals(NotificationScreen.WATER, NotificationScreen.fromKey("water"))
        assertEquals(NotificationScreen.GOALS, NotificationScreen.fromKey("goals"))
        assertEquals(NotificationScreen.SETTINGS, NotificationScreen.fromKey("settings"))
    }

    @Test
    fun keys_ignoreCaseAndSpaces() {
        assertEquals(NotificationScreen.SLEEP, NotificationScreen.fromKey("  SLEEP "))
    }

    @Test
    fun unknownOrMissingKeys_returnNull() {
        assertNull(NotificationScreen.fromKey("nowhere"))
        assertNull(NotificationScreen.fromKey(""))
        assertNull(NotificationScreen.fromKey(null))
    }

    @Test
    fun everyScreenKeyRoundTrips() {
        for (screen in NotificationScreen.values()) {
            assertEquals(screen, NotificationScreen.fromKey(screen.key))
        }
    }

    @Test
    fun channels_fallBackToUpdates() {
        assertEquals(NotificationChannels.ALERTS, NotificationChannels.idFor("alerts"))
        assertEquals(NotificationChannels.UPDATES, NotificationChannels.idFor("updates"))
        assertEquals(NotificationChannels.UPDATES, NotificationChannels.idFor(null))
        assertEquals(NotificationChannels.UPDATES, NotificationChannels.idFor("something-else"))
    }
}