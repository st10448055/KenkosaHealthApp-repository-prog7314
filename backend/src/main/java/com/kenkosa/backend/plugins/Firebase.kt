package com.kenkosa.backend.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream

fun Application.configureFirebase() {
    if (FirebaseApp.getApps().isEmpty()) {
        val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
            ?: "serviceAccountKey.json"

        val credentials = FileInputStream(serviceAccountPath).use { stream ->
            GoogleCredentials.fromStream(stream)
        }

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        FirebaseApp.initializeApp(options)
    }
}