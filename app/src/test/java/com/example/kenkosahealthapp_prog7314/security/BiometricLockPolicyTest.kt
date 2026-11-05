package com.example.kenkosahealthapp_prog7314.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// JVM unit tests for the biometric lock rules (run by GitHub Actions on every push).
class BiometricLockPolicyTest {

    @Test
    fun coldStart_locksOnlyWhenSessionAndBiometricEnabled() {
        assertTrue(BiometricLockPolicy.shouldLockOnColdStart(hasSession = true, biometricEnabled = true))
        assertFalse(BiometricLockPolicy.shouldLockOnColdStart(hasSession = true, biometricEnabled = false))
        assertFalse(BiometricLockPolicy.shouldLockOnColdStart(hasSession = false, biometricEnabled = true))
    }

    @Test
    fun relock_whenBackgroundedLongerThanTimeout() {
        assertTrue(BiometricLockPolicy.shouldRelock(true, true, backgroundedAtMs = 1_000, nowMs = 1_000 + 30_000))
    }

    @Test
    fun noRelock_whenBackIsWithinTimeout() {
        assertFalse(BiometricLockPolicy.shouldRelock(true, true, backgroundedAtMs = 1_000, nowMs = 1_000 + 29_999))
    }

    @Test
    fun noRelock_whenNeverBackgrounded() {
        assertFalse(BiometricLockPolicy.shouldRelock(true, true, backgroundedAtMs = 0, nowMs = 999_999))
    }

    @Test
    fun noRelock_whenBiometricDisabledOrNoSession() {
        assertFalse(BiometricLockPolicy.shouldRelock(true, false, 1_000, 100_000))
        assertFalse(BiometricLockPolicy.shouldRelock(false, true, 1_000, 100_000))
    }

    @Test
    fun customTimeoutIsRespected() {
        assertTrue(BiometricLockPolicy.shouldRelock(true, true, 1_000, 6_000, timeoutMs = 5_000))
    }

    @Test
    fun canEnable_onlyWhenAvailable() {
        assertTrue(BiometricLockPolicy.canEnable(BiometricAvailability.AVAILABLE))
        assertFalse(BiometricLockPolicy.canEnable(BiometricAvailability.NONE_ENROLLED))
        assertFalse(BiometricLockPolicy.canEnable(BiometricAvailability.NO_HARDWARE))
        assertFalse(BiometricLockPolicy.canEnable(BiometricAvailability.UNAVAILABLE))
    }
}

// References:
// Android Developers (n.d.) Build local unit tests. Available at:
//   https://developer.android.com/training/testing/local-tests (Accessed: 3 November 2026).
