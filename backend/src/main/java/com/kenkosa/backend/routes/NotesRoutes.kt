package com.kenkosa.backend.routes

import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.models.UpdateNoteRequest
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.NotesRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

fun Route.notesRoutes(notesRepository: NotesRepository) {
    // Requires Firebase authentication for all note routes.
    authenticate("firebase-auth") {
        route("/api/notes") {

            // Gets the note for the requested date, or today's date by default.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val date = call.request.queryParameters["date"] ?: LocalDate.now().toString()
                call.respond(HttpStatusCode.OK, notesRepository.getTodaysNote(principal.uid, date))
            }

            // Creates or updates the user's note for today.
            patch {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<UpdateNoteRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the note before saving it.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Saves the note using today's date.
                val date = LocalDate.now().toString()
                val updated = notesRepository.updateNote(principal.uid, date, request.noteText)
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //