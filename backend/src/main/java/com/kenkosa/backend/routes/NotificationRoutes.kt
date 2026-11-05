package com.kenkosa.backend.routes

import com.kenkosa.backend.models.AnnouncementRequest
import com.kenkosa.backend.models.AnnouncementResult
import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.models.RegisterDeviceRequest
import com.kenkosa.backend.models.SendResultResponse
import com.kenkosa.backend.models.UnregisterDeviceRequest
import com.kenkosa.backend.notifications.NotificationService
import com.kenkosa.backend.notifications.NotificationType
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.DeviceTokenRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest

fun Route.notificationRoutes(
    deviceRepository: DeviceTokenRepository,
    notificationService: NotificationService
) {

    authenticate("firebase-auth") {

        route("/api/devices/token") {

            put {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<RegisterDeviceRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@put call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }
                deviceRepository.register(principal.uid, request.token, request.platform)
                call.respond(HttpStatusCode.NoContent)
            }

            delete {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<UnregisterDeviceRequest>()
                } catch (e: Exception) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@delete call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }
                deviceRepository.unregister(principal.uid, request.token)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        // Sends a test push to the caller's own phones.
        post("/api/notifications/test") {
            val principal = call.principal<FirebaseUserPrincipal>()!!
            val outcome = notificationService.sendToUser(principal.uid, NotificationType.TEST)
            call.respond(HttpStatusCode.OK, SendResultResponse(devices = outcome.devices, sent = outcome.sent))
        }
    }

    // Admin broadcast, protected by the ADMIN_API_KEY environment variable.
    // If the variable is not set on the server, the endpoint is switched off.
    post("/api/admin/announcements") {
        val adminKey = System.getenv("ADMIN_API_KEY")
        if (adminKey.isNullOrBlank()) {
            return@post call.respond(HttpStatusCode.NotFound)
        }
        val supplied = call.request.headers["X-Admin-Key"].orEmpty()
        val authorised = MessageDigest.isEqual(supplied.toByteArray(), adminKey.toByteArray())
        if (!authorised) {
            return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse(listOf("Invalid admin key")))
        }

        val request = try {
            call.receive<AnnouncementRequest>()
        } catch (e: Exception) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
        }
        val errors = request.validate()
        if (errors.isNotEmpty()) {
            return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
        }

        val sent = notificationService.broadcast(request.title, request.body, request.screen)
        call.respond(HttpStatusCode.OK, AnnouncementResult(sent.keys.toList()))
    }
}