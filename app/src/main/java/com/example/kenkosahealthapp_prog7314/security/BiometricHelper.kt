package com.example.kenkosahealthapp_prog7314.security

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

// Wraps the AndroidX BiometricPrompt API (Google, 2024) so fragments only deal with
// simple callbacks. BIOMETRIC_WEAK is used so BOTH fingerprint and face unlock are accepted
// (many phones classify face unlock as "weak"; BIOMETRIC_STRONG would exclude them).
// minSdk is 33, so no legacy (pre-API 28) handling is required.
object BiometricHelper {

    private const val TAG = "BiometricHelper"
    private const val AUTHENTICATORS = BIOMETRIC_WEAK

    // Asks the OS whether biometrics can be used right now.
    fun checkAvailability(activity: FragmentActivity): BiometricAvailability {
        val result = when (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }
        Log.d(TAG, "Biometric availability = $result")
        return result
    }

    // Shows the system biometric dialog from a Fragment.
    // onSuccess: user authenticated. onCancelled: user pressed the negative button / back.
    // onError: lockout, hardware error, etc. (message is user-readable).
    fun authenticate(
        fragment: Fragment,
        title: String,
        subtitle: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = fragment.requireActivity()
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.i(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "Biometric error $errorCode: $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancelled()
                    else -> onError(errString.toString())   // lockout, no hardware, etc.
                }
            }

            // A single bad scan: the system dialog shows its own "try again" message,
            // so we only log it and let the user retry.
            override fun onAuthenticationFailed() {
                Log.d(TAG, "Biometric not recognised")
            }
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()

        BiometricPrompt(fragment, executor, callback).authenticate(info)
    }
}
