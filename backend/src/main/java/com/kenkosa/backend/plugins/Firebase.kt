package com.kenkosa.backend.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream

// Configures Firebase when the Ktor application starts.
fun Application.configureFirebase() {
    // Only initialise Firebase if it has not already been configured.
    if (FirebaseApp.getApps().isEmpty()) {
        // Get the service account path from the environment, or use the default file.
        val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
            ?: "serviceAccountKey.json"

        // Load the Firebase service account credentials from the file.
        val credentials = FileInputStream(serviceAccountPath).use { stream ->
            GoogleCredentials.fromStream(stream)
        }

        // Create Firebase options using the loaded credentials.
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        // Initialise the Firebase application with these settings.
        FirebaseApp.initializeApp(options)
    }
}

// ------------------ END OF FILE ------------------ //