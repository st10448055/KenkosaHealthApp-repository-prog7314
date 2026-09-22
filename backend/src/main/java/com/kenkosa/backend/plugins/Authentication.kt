package com.kenkosa.backend.plugins

import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*

// Stores the authenticated Firebase user's ID and email.
data class FirebaseUserPrincipal(val uid: String, val email: String?) : Principal

// Configures Firebase token authentication for the API.
fun Application.configureAuthentication() {
    install(Authentication) {
        bearer("firebase-auth") {
            realm = "Kenkosa Health API"
            authenticate { tokenCredential ->
                try {
                    // Verify the Firebase ID token and get the user's details.
                    val decodedToken = FirebaseAuth.getInstance()
                        .verifyIdToken(tokenCredential.token)

                    // Return the authenticated user's information.
                    FirebaseUserPrincipal(uid = decodedToken.uid, email = decodedToken.email)
                } catch (e: Exception) {
                    // Invalid, expired, revoked, or malformed token
                    null
                }
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //