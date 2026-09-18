package com.kenkosa.backend.routes

import com.kenkosa.backend.models.*
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.GoalRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.goalRoutes(repository: GoalRepository) {
    authenticate("firebase-auth") {
        route("/api/goals") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getGoalsForUser(principal.uid))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateGoalRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val created = repository.createGoal(principal.uid, request)
                call.respond(HttpStatusCode.Created, created)
            }

            patch("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val goalId = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(goalId)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Goal not found")))
                }

                val request = try {
                    call.receive<UpdateGoalRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateGoal(goalId, request)!!)
            }

            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val goalId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing goal id")))

                val existing = repository.getGoal(goalId)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Goal not found")))
                }

                repository.deleteGoal(goalId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}