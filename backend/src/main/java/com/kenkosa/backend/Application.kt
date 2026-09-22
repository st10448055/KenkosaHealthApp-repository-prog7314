package com.kenkosa.backend

import com.kenkosa.backend.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    // Starts the Ktor server using Netty on the configured port.
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Sets up Firebase, JSON serialization, authentication, error handling, and routes.
    configureFirebase()
    configureSerialization()
    configureAuthentication()
    configureStatusPages()
    configureRouting()
}

// ------------------ END OF FILE ------------------ //