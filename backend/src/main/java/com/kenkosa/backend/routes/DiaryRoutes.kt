package com.kenkosa.backend.routes

import com.kenkosa.backend.models.CreateMealEntryRequest
import com.kenkosa.backend.models.ErrorResponse
import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.MealDiaryRepository
import com.kenkosa.backend.repository.OpenFoodFactsClient
import com.kenkosa.backend.repository.UsdaFoodClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kenkosa.backend.models.CreateMealEntryRequest.UpdateMealEntryRequest

fun Route.diaryRoutes(
    mealRepository: MealDiaryRepository,
    openFoodFactsClient: OpenFoodFactsClient,
    usdaFoodClient: UsdaFoodClient
){
    // Requires Firebase authentication for all food and diary routes.
    authenticate("firebase-auth") {
        // Searches for food using a barcode or text query.
        route("/api/food/search"){
            get{
                val barcode = call.request.queryParameters["barcode"]
                val query = call.request.queryParameters["query"]

                // At least one search parameter must be provided.
                if(barcode.isNullOrBlank() && query.isNullOrBlank()){
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(listOf("Provide either a barcode or a query parameter"))
                    )
                }

                // Uses OpenFoodFacts when a barcode is provided.
                if(!barcode.isNullOrBlank()){
                    val offResult = openFoodFactsClient.lookupBarcode(barcode)
                    if (offResult != null)
                    {
                        return@get call.respond(HttpStatusCode.OK, offResult)

                    }

                    // Returns not found if the barcode has no matching food.
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(listOf("No food for this barcode"))
                    )
                }

                // Uses USDA to search for food by name or query.
                val result = usdaFoodClient.searchFood(query!!)
                if(result != null) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("No food found for this search")))
                }
            }
        }

        // Handles creating, viewing, updating and deleting diary entries.
        route("/api/diary"){
            get {
                // Gets the authenticated user's ID and optional date filter.
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val date = call.request.queryParameters["date"]
                call.respond(HttpStatusCode.OK, mealRepository.getMealsForUser(principal.uid, date))
            }

            post {
                // Reads the new meal entry from the request body.
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateMealEntryRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the meal data before saving it.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Creates the meal entry for the authenticated user.
                val created = mealRepository.createMealEntry(principal.uid, request)
                call.respond(HttpStatusCode.Created, created)
            }

            patch("/{id}") {
                // Gets the meal ID from the URL.
                val mealId = call.parameters["id"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id"))
                )

                // Reads the updated serving amount from the request.
                val request = try {
                    call.receive<UpdateMealEntryRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                // Validates the updated meal data.
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                // Updates the servings and recalculates the meal values.
                val updated = mealRepository.updateMealEntryServings(mealId, request.servings)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Meal entry not found")))
                }
            }

            delete("/{id}") {
                // Gets the meal ID that should be deleted.
                val mealId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id"))
                )

                // Deletes the meal entry and returns the appropriate result.
                val deleted = mealRepository.deleteMealEntry(mealId)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Meal entry not found")))
                }
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //