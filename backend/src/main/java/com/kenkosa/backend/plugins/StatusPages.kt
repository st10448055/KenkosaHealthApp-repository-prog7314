package com.kenkosa.backend.plugins

import com.kenkosa.backend.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

// Handles unexpected errors that occur while processing API requests.
fun Application.configureStatusPages() {
    install(StatusPages) {
        // Returns a 500 error with the exception message when an unhandled error occurs.
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(listOf(cause.message ?: "Unexpected server error"))
            )
        }
    }
}

// ------------------ END OF FILE ------------------ //