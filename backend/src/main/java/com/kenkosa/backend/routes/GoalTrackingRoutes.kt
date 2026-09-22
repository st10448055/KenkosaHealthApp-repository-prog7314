package com.kenkosa.backend.routes

import com.kenkosa.backend.models.*
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.GoalTrackingRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.goalTrackingRoutes(repository: GoalTrackingRepository) {
    authenticate("firebase-auth") {
        route("/api/goal-tracking") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getGoalsForUser(principal.uid))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<SaveTrackedGoalRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createGoal(principal.uid, request))
            }

            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            put("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                val request = try {
                    call.receive<SaveTrackedGoalRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateGoal(id, request)!!)
            }

            put("/{id}/progress") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                val request = try {
                    call.receive<LogProgressRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.logProgress(id, request.date, request.value)!!)
            }

            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                repository.deleteGoal(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}