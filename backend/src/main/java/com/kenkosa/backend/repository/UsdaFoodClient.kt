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
    suspend fun searchFood(query: String): FoodItemResponse? {
        // Returns null instead of failing if the API key is not configured.
        if(apiKey.isEmpty()) return null //fail safe rather than a crash if the key isn't configured

        // Sends the food search request to the USDA API.
        val response: UsdaSearchResponse = client
            .get("https://api.nal.usda.gov/fdc/v1/foods/search"){
                url{
                    parameters.append("query", query)
                    parameters.append("api_key", apiKey)
                    parameters.append("pageSize", "1")
                }
            }
            .body()

        // Uses the first matching food returned by the search.
        val food = response.foods.firstOrNull() ?: return null

        // Gets the main nutrition values from the USDA response.
        val calories = food.findNutrient("Energy") ?: 0.0
        val protein = food.findNutrient("Protein") ?: 0.0
        val carbs = food.findNutrient("Carbohydrate, by difference") ?: 0.0
        val fat = food.findNutrient("Total lipid (fat)") ?: 0.0

        // Maps the USDA food data into the format used by the application.
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

    // Finds a specific nutrient by its USDA nutrient name.
    private fun UsdaFood.findNutrient(name: String): Double? =
        foodNutrients.firstOrNull { it.nutrientName == name }?.value

    // Closes the HTTP client when it is no longer needed.
    fun close() = client.close()
}

// ------------------ END OF FILE ------------------ //