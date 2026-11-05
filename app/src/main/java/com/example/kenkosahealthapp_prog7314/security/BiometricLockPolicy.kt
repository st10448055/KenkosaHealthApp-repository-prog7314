package com.example.kenkosahealthapp_prog7314.security

// Pure Kotlin rules deciding WHEN the app must ask for biometric verification.
// Kept free of Android classes so it can be unit tested on the JVM (see BiometricLockPolicyTest).
object BiometricLockPolicy {

    // How long the app may stay in the background before it re-locks (30 seconds).
    const val RELOCK_TIMEOUT_MS = 30_000L

    // Cold start: the app process was just created. Lock if a session exists
    // and the user turned biometric login on.
    fun shouldLockOnColdStart(hasSession: Boolean, biometricEnabled: Boolean): Boolean =
        hasSession && biometricEnabled

    // Returning from background: lock only if enabled, a session exists,
    // we were backgrounded at least once, and the timeout has elapsed.
    fun shouldRelock(
        hasSession: Boolean,
        biometricEnabled: Boolean,
        backgroundedAtMs: Long,
        nowMs: Long,
        timeoutMs: Long = RELOCK_TIMEOUT_MS
    ): Boolean {
        if (!hasSession || !biometricEnabled) return false
        if (backgroundedAtMs <= 0L) return false          // never backgrounded
        return nowMs - backgroundedAtMs >= timeoutMs
    }

    // Biometric login can only be turned on when the device has enrolled biometrics.
    fun canEnable(availability: BiometricAvailability): Boolean =
        availability == BiometricAvailability.AVAILABLE
}

// Result of asking the device whether biometrics can be used.
enum class BiometricAvailability {
    AVAILABLE,        // hardware present and at least one fingerprint/face enrolled
    NONE_ENROLLED,    // hardware present but nothing enrolled
    NO_HARDWARE,      // device has no biometric sensor
    UNAVAILABLE       // temporarily unavailable / security update needed / unsupported
}

// References:
// Android Developers (n.d.) Show a biometric authentication dialog. Available at:
//   https://developer.android.com/identity/sign-in/biometric-auth (Accessed: 3 November 2026).
// Android Developers (n.d.) Build local unit tests. Available at:
//   https://developer.android.com/training/testing/local-tests (Accessed: 3 November 2026).
