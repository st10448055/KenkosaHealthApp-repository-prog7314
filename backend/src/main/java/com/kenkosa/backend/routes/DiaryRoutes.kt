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
        // Text search: returns several USDA matches so the user can choose between them.
        route("/api/food/results") {
            get {
                val query = call.request.queryParameters["query"]
                if (query.isNullOrBlank()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("query is required")))
                }
                call.respond(HttpStatusCode.OK, usdaFoodClient.searchFoods(query.trim()))
            }
        }

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
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val mealId = call.parameters["id"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id")))

                // Ownership check: another user's entry looks like "not found", never "forbidden"
                val existing = mealRepository.getMealEntry(mealId)
                if (existing == null || existing.userId != principal.uid) {
                    return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Meal entry not found")))
                }

                val request = try { call.receive<UpdateMealEntryRequest>() } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }
                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }
                call.respond(HttpStatusCode.OK, mealRepository.updateMealEntryServings(mealId, request.servings)!!)
            }

            delete("/{id}") {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val mealId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id")))

                val existing = mealRepository.getMealEntry(mealId)
                if (existing == null || existing.userId != principal.uid) {
                    return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Meal entry not found")))
                }
                mealRepository.deleteMealEntry(mealId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ------------------ END OF FILE ------------------ //