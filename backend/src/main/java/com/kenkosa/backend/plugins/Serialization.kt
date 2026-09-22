package com.kenkosa.backend.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

// Configures JSON serialization for requests and responses.
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            // Makes JSON responses easier to read during development.
            prettyPrint = true

            // Allows more flexible JSON formatting when processing requests.
            isLenient = true

            // Ignores extra JSON fields that are not defined in the data models.
            ignoreUnknownKeys = true
        })
    }
}

// ------------------ END OF FILE ------------------ //