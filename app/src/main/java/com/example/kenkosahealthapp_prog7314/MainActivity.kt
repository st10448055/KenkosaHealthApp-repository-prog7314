package com.example.kenkosahealthapp_prog7314

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.kenkosahealthapp_prog7314.ui.settings.ThemeManager
import com.google.firebase.auth.FirebaseAuth

// MainActivity is the main entry point of the Kenkosa Health application.
// It hosts the application's navigation and manages the initial screen.
class MainActivity : AppCompatActivity() {

    // Called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {

        // Apply the user's previously saved theme before
        // creating the activity's views.
        ThemeManager.applySaved(this)

        // Call the parent implementation of onCreate().
        super.onCreate(savedInstanceState)

        // Load the activity's layout from activity_main.xml.
        setContentView(R.layout.activity_main)

        // Find the NavHostFragment from the activity layout.
        // The NavHostFragment is responsible for displaying
        // the different fragments used by the application.
        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.nav_host_fragment
            ) as NavHostFragment

        // Retrieve the NavController responsible for
        // navigating between the application's destinations.
        val navController = navHostFragment.navController

        // Only perform the initial navigation when the activity
        // is being created for the first time.
        // FirebaseAuth.currentUser checks whether a user is
        // already authenticated.
        // If a user is logged in, navigate directly to the
        // Home screen instead of displaying the login screen.
        if (
            savedInstanceState == null &&
            FirebaseAuth.getInstance().currentUser != null
        ) {

            // Navigate to the HomeFragment.
            navController.navigate(
                R.id.homeFragment,
                null,

                // Configure the navigation behaviour.
                androidx.navigation.navOptions {

                    // Remove the previous navigation graph from
                    // the back stack so that the user cannot press
                    // Back to return to the login/start screen.
                    popUpTo(R.id.nav_graph) {
                        inclusive = true
                    }
                }
            )
        }
    }
}
// ------------------ END OF FILE ------------------ //