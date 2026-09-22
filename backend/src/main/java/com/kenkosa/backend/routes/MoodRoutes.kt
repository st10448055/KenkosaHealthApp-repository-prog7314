package com.kenkosa.backend.routes

import com.kenkosa.backend.models.*
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.MoodRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.moodRoutes(repository: MoodRepository) {
    // Requires Firebase authentication for all mood routes.
    authenticate("firebase-auth") {
        route("/api/moods") {

            // Gets all mood entries for the authenticated user.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getMoodsForUser(principal.uid))
            }

            // Creates a new mood entry.
            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateMoodRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the mood details before saving.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createMood(principal.uid, request))
            }

            // Gets a specific mood entry.
            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing mood id")))

                // Checks that the mood entry exists and belongs to the current user.
                val existing = repository.getMood(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Mood entry not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            // Updates an existing mood entry.
            patch("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing mood id")))

                // Prevents users from updating another user's mood entry.
                val existing = repository.getMood(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Mood entry not found")))
                }

                val request = try {
                    call.receive<UpdateMoodRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the updated mood details.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateMood(id, request)!!)
            }

            // Deletes an existing mood entry.
            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing mood id")))

                // Checks that the mood entry belongs to the authenticated user.
                val existing = repository.getMood(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Mood entry not found")))
                }

                // Removes the mood entry from the repository.
                repository.deleteMood(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //