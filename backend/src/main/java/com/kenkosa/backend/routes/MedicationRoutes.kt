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
    // Requires Firebase authentication for all medication routes.
    authenticate("firebase-auth") {
        route("/api/medications") {

            // Gets all medications for the authenticated user.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                call.respond(HttpStatusCode.OK, repository.getMedicationsForUser(principal.uid))
            }

            // Creates a new medication record.
            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateMedicationRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the medication details before saving.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.Created, repository.createMedication(principal.uid, request))
            }

            // Gets a specific medication.
            get("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                // Checks that the medication exists and belongs to the current user.
                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@get call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }
                call.respond(HttpStatusCode.OK, existing)
            }

            // Updates an existing medication.
            patch("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                // Prevents users from updating another user's medication.
                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                val request = try {
                    call.receive<UpdateMedicationRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the updated medication details.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                call.respond(HttpStatusCode.OK, repository.updateMedication(id, request)!!)
            }

            // Records whether the medication was taken on a specific date.
            put("/{id}/taken") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                // Checks that the medication belongs to the authenticated user.
                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@put call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                val request = try {
                    call.receive<SetTakenRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the taken status and date.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Saves or removes the medication date based on the taken value.
                call.respond(HttpStatusCode.OK, repository.setTaken(id, request.date, request.taken)!!)
            }

            // Deletes an existing medication.
            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing medication id")))

                // Checks that the medication belongs to the authenticated user.
                val existing = repository.getMedication(id)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Medication not found")))
                }

                // Removes the medication from the repository.
                repository.deleteMedication(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //