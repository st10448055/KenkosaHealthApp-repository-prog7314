package com.kenkosa.backend.routes

import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.repository.WgerClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.workoutRoutes(wgerClient: WgerClient) {
    authenticate("firebase-auth") {

        route("/api/workouts") {

            get("/categories") {
                val categories = wgerClient.getCategories()
                call.respond(HttpStatusCode.OK, categories)
            }

            get {
                val categoryId = call.request.queryParameters["category"]?.toIntOrNull()

                if (categoryId == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(listOf("Provide a category query parameter (integer id)"))
                    )
                }

                val exercises = wgerClient.getExercisesByCategory(categoryId)
                call.respond(HttpStatusCode.OK, exercises)
            }
        }
    }
}