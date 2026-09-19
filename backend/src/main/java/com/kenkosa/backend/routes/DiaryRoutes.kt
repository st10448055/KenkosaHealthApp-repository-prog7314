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
    authenticate("firebase-auth") {
        route("/api/food/search"){
            get{
                val barcode = call.request.queryParameters["barcode"]
                val query = call.request.queryParameters["query"]

                if(barcode.isNullOrBlank() && query.isNullOrBlank()){
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(listOf("Provide either a barcode or a query parameter"))
                    )
                }

                if(!barcode.isNullOrBlank()){
                    val offResult = openFoodFactsClient.lookupBarcode(barcode)
                    if (offResult != null)
                    {
                        return@get call.respond(HttpStatusCode.OK, offResult)

                    }

                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(listOf("No food for this barcode"))
                    )
                }

                val result = usdaFoodClient.searchFood(query!!)
                if(result != null) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("No food found for this search")))
                }
            }
        }

        route("/api/diary"){
            get {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val date = call.request.queryParameters["date"]
                call.respond(HttpStatusCode.OK, mealRepository.getMealsForUser(principal.uid, date))
            }

            post {
                val principal = call.principal<FirebaseUserPrincipal>()!!
                val request = try {
                    call.receive<CreateMealEntryRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val created = mealRepository.createMealEntry(principal.uid, request)
                call.respond(HttpStatusCode.Created, created)
            }

            patch("/{id}") {
                val mealId = call.parameters["id"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id"))
                )

                val request = try {
                    call.receive<UpdateMealEntryRequest>()
                } catch (e: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf("Malformed request body")))
                }

                val errors = request.validate()
                if (errors.isNotEmpty()) {
                    return@patch call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(errors))
                }

                val updated = mealRepository.updateMealEntryServings(mealId, request.servings)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf("Meal entry not found")))
                }
            }

            delete("/{id}") {
                val mealId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse(listOf("Missing meal id"))
                )

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