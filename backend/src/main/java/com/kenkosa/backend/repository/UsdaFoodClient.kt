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

// Represents the USDA food search response.
@Serializable
private data class UsdaSearchResponse(
    val foods: List<UsdaFood> = emptyList()
)

// Represents a food item returned by the USDA API.
@Serializable private data class UsdaFood(
    val description: String? = null,
    @SerialName("fdcId") val fdcId: Int? = null,
    @SerialName("foodNutrients") val foodNutrients: List<UsdaNutrient> = emptyList()

)

// Represents a nutrient returned for a USDA food item.
@Serializable
private data class UsdaNutrient(
    @SerialName("nutrientName") val nutrientName: String? = null,
    @SerialName("unitName") val unitName: String? = null,
    val value: Double? = null
)


class UsdaFoodClient {
    // Reads the USDA API key from the environment.
    private val apiKey = System.getenv("USDA_API_KEY") ?: ""

    // HTTP client used to communicate with the USDA API.
    private val client = HttpClient(CIO){
        install(ContentNegotiation){
            json(Json {ignoreUnknownKeys = true})
        }
    }

    // Searches the USDA database and converts the result into the app's food model.
    suspend fun searchFoods(query: String, limit: Int = 10): List<FoodItemResponse> {
        if (apiKey.isEmpty()) return emptyList()
        val response: UsdaSearchResponse = client
            .get("https://api.nal.usda.gov/fdc/v1/foods/search") {
                url {
                    parameters.append("query", query)
                    parameters.append("api_key", apiKey)
                    parameters.append("pageSize", limit.toString())
                }
            }.body()

        return response.foods.map { food ->
            FoodItemResponse(
                foodId = food.fdcId?.toString() ?: query,
                name = food.description ?: query,
                barcode = null,
                servingSizeG = 100.0,
                calories = food.foodNutrients.firstOrNull { it.nutrientName == "Energy" && it.unitName == "KCAL" }?.value
                    ?: food.findNutrient("Energy") ?: 0.0,
                proteinG = food.findNutrient("Protein") ?: 0.0,
                carbsG = food.findNutrient("Carbohydrate, by difference") ?: 0.0,
                fatG = food.findNutrient("Total lipid (fat)") ?: 0.0,
                source = "USDA"
            )
        }
    }

    suspend fun searchFood(query: String): FoodItemResponse? = searchFoods(query, 1).firstOrNull()

    // Finds a specific nutrient by its USDA nutrient name.
    private fun UsdaFood.findNutrient(name: String): Double? =
        foodNutrients.firstOrNull { it.nutrientName == name }?.value

    // Closes the HTTP client when it is no longer needed.
    fun close() = client.close()
}

// ------------------ END OF FILE ------------------ //