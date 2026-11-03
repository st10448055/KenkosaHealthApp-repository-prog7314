package com.example.kenkosahealthapp_prog7314

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.kenkosahealthapp_prog7314.security.BiometricLockPolicy
import com.example.kenkosahealthapp_prog7314.security.BiometricPreferences
import com.example.kenkosahealthapp_prog7314.ui.auth.BiometricLockFragment
import com.example.kenkosahealthapp_prog7314.ui.settings.ThemeManager
import com.google.firebase.auth.FirebaseAuth

// MainActivity is the main entry point of the Kenkosa Health application.
// It hosts the application's navigation and decides the first screen:
//  - not signed in                          Start screen
//  - signed in, biometric login ON          Biometric lock screen
//  - signed in, biometric login OFF         Home
// It also re-locks the app when it returns from the background after a timeout.
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var biometricPrefs: BiometricPreferences

    // Called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {

        // Apply the user's previously saved theme before creating the activity's views.
        ThemeManager.applySaved(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate (savedInstanceState null = ${savedInstanceState == null})")

        biometricPrefs = BiometricPreferences(this)

        // A fresh launch must not inherit a stale "backgrounded at" time from a previous run,
        // otherwise onStart could stack a second lock screen on top of the cold-start one.
        if (savedInstanceState == null) biometricPrefs.backgroundedAtMs = 0L

        // The NavHostFragment displays the different fragments used by the application.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Only perform the initial navigation when the activity is created for the first time.
        val user = FirebaseAuth.getInstance().currentUser
        if (savedInstanceState == null && user != null) {

            val lock = BiometricLockPolicy.shouldLockOnColdStart(
                hasSession = true,
                biometricEnabled = biometricPrefs.isEnabled(user.uid)
            )
            Log.d(TAG, "Existing session found, lock required = $lock")

            // Biometric ON -> lock screen first; OFF -> straight to Home.
            // popUpTo removes the login graph so Back cannot return to it.
            navController.navigate(
                if (lock) R.id.biometricLockFragment else R.id.homeFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
            )
        }
    }

    // The app left the foreground: remember when, so onStart can apply the re-lock timeout.
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            biometricPrefs.backgroundedAtMs = System.currentTimeMillis()
            Log.d(TAG, "App backgrounded")
        }
    }

    // The app returned to the foreground: lock again if it was away longer than the timeout.
    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val destination = navController.currentDestination?.id
        // Never stack a lock screen on top of the lock screen or the sign-in screens.
        val onAuthScreen = destination in setOf(
            R.id.biometricLockFragment, R.id.startFragment, R.id.ssoFragment,
            R.id.loginFragment, R.id.signUpFragment
        )
        if (onAuthScreen) return

        val relock = BiometricLockPolicy.shouldRelock(
            hasSession = true,
            biometricEnabled = biometricPrefs.isEnabled(user.uid),
            backgroundedAtMs = biometricPrefs.backgroundedAtMs,
            nowMs = System.currentTimeMillis()
        )
        if (relock) {
            Log.i(TAG, "Re-locking app after background timeout")
            biometricPrefs.backgroundedAtMs = 0L
            navController.navigate(
                R.id.action_global_biometricLock,
                bundleOf(BiometricLockFragment.ARG_RELOCK to true)
            )
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
// ------------------ END OF FILE ------------------ //