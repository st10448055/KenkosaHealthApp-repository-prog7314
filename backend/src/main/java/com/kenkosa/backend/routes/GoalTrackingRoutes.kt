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
import com.kenkosa.backend.notifications.NotificationService
import com.kenkosa.backend.notifications.NotificationType
import java.time.LocalDate

fun Route.goalTrackingRoutes(repository: GoalTrackingRepository, notifications: NotificationService) {
    // Requires Firebase authentication for all goal tracking routes.
    authenticate("firebase-auth") {
        route("/api/goal-tracking") {

            // Gets all tracked goals for the authenticated user.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getGoalsForUser(principal.uid))
            }

            // Creates a new tracked goal.
            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<SaveTrackedGoalRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                // Validates the goal before saving it.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createGoal(principal.uid, request))
            }

            // Gets a specific tracked goal.
            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                // Makes sure the goal exists and belongs to the current user.
                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            // Updates the details of an existing goal.
            put("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                // Prevents users from updating another user's goal.
                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                val request = try {
                    call.receive<SaveTrackedGoalRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                // Validates the updated goal details.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateGoal(id, request)!!)
            }

            // Records progress for an existing goal.
            put("/{id}/progress") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                // Checks that the goal exists and belongs to the current user.
                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                val request = try {
                    call.receive<LogProgressRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Malformed request body")))
                }

                // Validates the progress entry before saving it.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, TrackingErrorResponse(errors))
                }

                val updated = repository.logProgress(id, request.date, request.value)!!

                // Notify only the first time today's progress crosses the target (never for back-dated entries).
                val target = existing.targetValue
                val previous = existing.progressLog[request.date] ?: 0.0
                val isToday = request.date == LocalDate.now().toString()
                if (isToday && target > 0.0 && previous < target && request.value >= target) {
                    notifications.sendToUserAsync(principal.uid, NotificationType.GOAL_COMPLETED, existing.name)
                }
                call.respond(HttpStatusCode.OK, updated)
            }

            // Deletes an existing tracked goal.
            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, TrackingErrorResponse(listOf("Missing goal id")))

                // Checks that the goal belongs to the authenticated user.
                val existing = repository.getGoal(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, TrackingErrorResponse(listOf("Goal not found")))
                }

                // Removes the goal from the repository.
                repository.deleteGoal(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //