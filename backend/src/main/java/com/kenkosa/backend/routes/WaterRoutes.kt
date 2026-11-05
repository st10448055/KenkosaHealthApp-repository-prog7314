package com.kenkosa.backend.routes

import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.models.UpdateWaterRequest
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.WaterRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import com.kenkosa.backend.notifications.NotificationService
import com.kenkosa.backend.notifications.NotificationType

fun Route.waterRoutes(waterRepository: WaterRepository, notifications: NotificationService) {

    // Requires Firebase authentication for all water routes.
    authenticate("firebase-auth") {
        route("/api/water") {

            // Gets the user's water intake for the requested date or today by default.
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val date = call.request.queryParameters["date"] ?: LocalDate.now().toString()
                call.respond(HttpStatusCode.OK, waterRepository.getTodaysWater(principal.uid, date))
            }

            // Updates today's water intake after validating the request.
            patch {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<UpdateWaterRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Checks that the water intake value is valid before saving it.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val date = LocalDate.now().toString()

                // What the user had before this update, so we only notify when the goal is crossed.
                val before = waterRepository.getTodaysWater(principal.uid, date)
                val updated = waterRepository.updateWater(principal.uid, date, request.cupsConsumed)

                if (before.cupsConsumed < before.targetCups && updated.cupsConsumed >= updated.targetCups) {
                    notifications.sendToUserAsync(principal.uid, NotificationType.WATER_GOAL_REACHED)
                }
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //