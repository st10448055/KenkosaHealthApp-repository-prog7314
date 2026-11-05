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
import android.content.Intent
import com.example.kenkosahealthapp_prog7314.notifications.NotificationHelper
import com.example.kenkosahealthapp_prog7314.notifications.NotificationScreen

// MainActivity is the main entry point of the Kenkosa Health application.
// It hosts the application's navigation and decides the first screen:
//  - not signed in                          Start screen
//  - signed in, biometric login ON          Biometric lock screen
//  - signed in, biometric login OFF         Home
// It also re-locks the app when it returns from the background after a timeout.
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var biometricPrefs: BiometricPreferences

    // Screen requested by a tapped notification, waiting until the user is past the lock/sign-in screens.
    private var pendingScreen: NotificationScreen? = null

    private val authDestinations = setOf(
        R.id.biometricLockFragment, R.id.startFragment, R.id.ssoFragment,
        R.id.loginFragment, R.id.signUpFragment
    )

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

        // Opens a tapped notification's screen once the user is on a normal (unlocked) screen.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (pendingScreen != null && destination.id !in authDestinations) openPendingScreen()
        }
// The app was started by tapping a notification.
        if (savedInstanceState == null) handleNotificationIntent(intent)

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

    // The app was already running and the user tapped a notification.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val key = intent.getStringExtra(NotificationHelper.EXTRA_SCREEN) ?: return
        intent.removeExtra(NotificationHelper.EXTRA_SCREEN)   // so it is not handled twice
        if (FirebaseAuth.getInstance().currentUser == null) return
        pendingScreen = NotificationScreen.fromKey(key) ?: return
        openPendingScreen()
    }

    // Navigates unless the user is still on a sign-in / lock screen (a notification must never bypass the lock).
    private fun openPendingScreen() {
        window.decorView.post {
            val screen = pendingScreen ?: return@post
            val current = navController.currentDestination?.id ?: return@post
            if (current in authDestinations) return@post
            pendingScreen = null

            val target = when (screen) {
                NotificationScreen.HOME -> R.id.homeFragment
                NotificationScreen.WATER -> R.id.waterFragment
                NotificationScreen.GOALS -> R.id.goalsFragment
                NotificationScreen.SLEEP -> R.id.sleepFragment
                NotificationScreen.FASTING -> R.id.fastingFragment
                NotificationScreen.MEDICATION -> R.id.medicationFragment
                NotificationScreen.WELLNESS -> R.id.wellnessFragment
                NotificationScreen.SETTINGS -> R.id.settingsFragment
            }
            if (current == target) return@post

            // Start from Home so Back behaves normally, then open the target.
            navController.popBackStack(R.id.homeFragment, false)
            if (target != R.id.homeFragment) navController.navigate(target)
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