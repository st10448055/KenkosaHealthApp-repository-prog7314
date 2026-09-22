package com.kenkosa.backend.routes

import com.kenkosa.backend.models.*
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.FastRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val ALREADY_ACTIVE = "You already have an active fast. End it before starting another."

fun Route.fastRoutes(repository: FastRepository) {
    // Requires Firebase authentication for all fasting routes.
    authenticate("firebase-auth") {
        route("/api/fasts") {

            // Gets all fasting records for the authenticated user.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getFastsForUser(principal.uid))
            }

            // Creates a new fasting record.
            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateFastRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the fasting details before creating the record.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Only one fast can be in progress at a time.
                if (request.isActive() && repository.hasActiveFast(principal.uid)) {
                    return@post call.respond(HttpStatusCode.Conflict, ErrorResponse(listOf(ALREADY_ACTIVE)))
                }

                // Saves the new fast for the authenticated user.
                call.respond(HttpStatusCode.Created, repository.createFast(principal.uid, request))
            }

            // Gets a specific fasting record.
            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing fast id")))

                // Checks that the fast exists and belongs to the current user.
                val existing = repository.getFast(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Fast not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            // Updates an existing fasting record.
            patch("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing fast id")))

                // Prevents users from updating another user's fast.
                val existing = repository.getFast(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Fast not found")))
                }

                val request = try {
                    call.receive<UpdateFastRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validate the fast as it will look after the change, not just the fields that were sent.
                val startTime = request.startTime ?: existing.startTime
                val endTime = request.endTime ?: existing.endTime
                val active = request.active ?: existing.active

                // Validates the combined existing and updated values.
                val errors = validateFastTimes(startTime, endTime, active)
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Prevents a second active fast from being started.
                if (active && !existing.active && repository.hasActiveFast(principal.uid, exceptFastId = id)) {
                    return@patch call.respond(HttpStatusCode.Conflict, ErrorResponse(listOf(ALREADY_ACTIVE)))
                }

                // Saves the updated fasting record.
                call.respond(HttpStatusCode.OK, repository.updateFast(id, startTime, endTime, active)!!)
            }

            // Deletes an existing fasting record.
            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing fast id")))

                // Checks that the fast belongs to the authenticated user.
                val existing = repository.getFast(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Fast not found")))
                }

                // Removes the fasting record.
                repository.deleteFast(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //