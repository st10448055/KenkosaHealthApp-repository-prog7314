package com.kenkosa.backend.routes

import com.kenkosa.backend.models.SaveSleepLogRequest
import com.kenkosa.backend.models.SleepLogErrorResponse
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.SleepLogRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Full CRUD for the Sleep screens. Lives under its own path so it can't clash with the original /api/sleep. */
fun Route.sleepLogRoutes(repository: SleepLogRepository) {
    authenticate("firebase-auth") {
        route("/api/sleep-log") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getSleepForUser(principal.uid))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<SaveSleepLogRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, SleepLogErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, SleepLogErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createSleep(principal.uid, request))
            }

            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, SleepLogErrorResponse(listOf("Missing sleep id")))

                val existing = repository.getSleep(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, SleepLogErrorResponse(listOf("Sleep entry not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            put("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, SleepLogErrorResponse(listOf("Missing sleep id")))

                val existing = repository.getSleep(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, SleepLogErrorResponse(listOf("Sleep entry not found")))
                }

                val request = try {
                    call.receive<SaveSleepLogRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, SleepLogErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, SleepLogErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateSleep(id, request)!!)
            }

            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, SleepLogErrorResponse(listOf("Missing sleep id")))

                val existing = repository.getSleep(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, SleepLogErrorResponse(listOf("Sleep entry not found")))
                }

                repository.deleteSleep(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
