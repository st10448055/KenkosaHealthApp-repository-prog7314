package com.kenkosa.backend.plugins

import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*

data class FirebaseUserPrincipal(val uid: String, val email: String?) : Principal

fun Application.configureAuthentication() {
    install(Authentication) {
        bearer("firebase-auth") {
            realm = "Kenkosa Health API"
            authenticate { tokenCredential ->
                try {
                    val decodedToken = FirebaseAuth.getInstance()
                        .verifyIdToken(tokenCredential.token)
                    FirebaseUserPrincipal(uid = decodedToken.uid, email = decodedToken.email)
                } catch (e: Exception) {
                    // Invalid, expired, revoked, or malformed token
                    null
                }
            }
        }
    }
}