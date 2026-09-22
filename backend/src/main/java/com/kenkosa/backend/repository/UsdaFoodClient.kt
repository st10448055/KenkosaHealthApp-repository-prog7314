package com.kenkosa.backend.repository

import com.kenkosa.backend.models.FoodItemResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
private data class UsdaSearchResponse(
    val foods: List<UsdaFood> = emptyList()
)

@Serializable private data class UsdaFood(
    val description: String? = null,
    @SerialName("fdcId") val fdcId: Int? = null,
    @SerialName("foodNutrients") val foodNutrients: List<UsdaNutrient> = emptyList()

)

@Serializable
private data class UsdaNutrient(
    @SerialName("nutrientName") val nutrientName: String? = null,
    val value: Double? = null
)

class UsdaFoodClient {
    private val apiKey = System.getenv("USDA_API_KEY") ?: ""

    private val client = HttpClient(CIO){
        install(ContentNegotiation){
            json(Json {ignoreUnknownKeys = true})
        }
    }

    suspend fun searchFood(query: String): FoodItemResponse? {
        if(apiKey.isEmpty()) return null //fail safe rather than a crash if the key isn't configured

        val response: UsdaSearchResponse = client
            .get("https://api.nal.usda.gov/fdc/v1/foods/search"){
                url{
                    parameters.append("query", query)
                    parameters.append("api_key", apiKey)
                    parameters.append("pageSize", "1")
                }
            }
            .body()

        val food = response.foods.firstOrNull() ?: return null

        val calories = food.findNutrient("Energy") ?: 0.0
        val protein = food.findNutrient("Protein") ?: 0.0
        val carbs = food.findNutrient("Carbohydrate, by difference") ?: 0.0
        val fat = food.findNutrient("Total lipid (fat)") ?: 0.0

        return FoodItemResponse(
            foodId = food.fdcId?.toString() ?: query,
            name = food.description ?: query,
            barcode = null,
            servingSizeG = 100.0,
            calories = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            source = "USDA"
        )

    }

    private fun UsdaFood.findNutrient(name: String): Double? =
        foodNutrients.firstOrNull { it.nutrientName == name }?.value

    fun close() = client.close()
}
