package com.kenkosa.backend.routes

import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.repository.WgerClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.workoutRoutes(wgerClient: WgerClient) {

    // Requires Firebase authentication for all workout routes.
    authenticate("firebase-auth") {

        route("/api/workouts") {

            // Gets the available workout categories from the Wger API.
            get("/categories") {
                val categories = wgerClient.getCategories()
                call.respond(HttpStatusCode.OK, categories)
            }

            // Gets exercises for the selected workout category.
            get {
                val categoryId = call.request.queryParameters["category"]?.toIntOrNull()

                // Requires a valid category ID before requesting exercises.
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

// ------------------ END OF FILE ------------------ //