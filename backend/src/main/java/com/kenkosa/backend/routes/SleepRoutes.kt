package com.kenkosa.backend.routes

import com.kenkosa.backend.models.CreateSleepRequest
import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.SleepRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sleepRoutes(repository: SleepRepository) {
    authenticate("firebase-auth") {
        route("/api/sleep") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val from = call.request.queryParameters["from"]?.toLongOrNull()
                val to = call.request.queryParameters["to"]?.toLongOrNull()
                call.respond(HttpStatusCode.OK, repository.getSleepForUser(principal.uid, from, to))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateSleepRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val created = repository.createSleepRecord(principal.uid, request)
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}