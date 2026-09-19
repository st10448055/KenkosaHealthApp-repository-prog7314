package com.kenkosa.backend.routes

import com.kenkosa.backend.models.*
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.MedicationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.medicationRoutes(repository: MedicationRepository) {
    authenticate("firebase-auth") {
        route("/api/medications") {

            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getMedicationsForUser(principal.uid))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateMedicationRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createMedication(principal.uid, request))
            }

            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            patch("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                val request = try {
                    call.receive<UpdateMedicationRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateMedication(id, request)!!)
            }

            put("/{id}/taken") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                val request = try {
                    call.receive<SetTakenRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.setTaken(id, request.date, request.taken)!!)
            }

            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                repository.deleteMedication(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
