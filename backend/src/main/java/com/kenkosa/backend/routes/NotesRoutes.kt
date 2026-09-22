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
    authenticate("firebase-auth") {
        route("/api/notes") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val date = call.request.queryParameters["date"] ?: LocalDate.now().toString()
                call.respond(HttpStatusCode.OK, notesRepository.getTodaysNote(principal.uid, date))
            }

            patch {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<UpdateNoteRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val date = LocalDate.now().toString()
                val updated = notesRepository.updateNote(principal.uid, date, request.noteText)
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}