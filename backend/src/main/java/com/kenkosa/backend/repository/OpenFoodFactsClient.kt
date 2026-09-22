package com.kenkosa.backend.repository

import com.kenkosa.backend.models.FoodItemResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

// Response models used to read product information from OpenFoodFacts.
@Serializable
private data class OffProductResponse(
    val status: Int,
    val product: OffProduct? = null
)

@Serializable
private data class OffProduct(
    val product_name: String? = null,
    val nutriments: OffNutriments? = null,
    val serving_quantity: Double? = null
)

@Serializable
private data class OffNutriments(
    val `energy-kcal_100g`: Double? = null,
    val proteins_100g: Double? = null,
    val carbohydrates_100g: Double? = null,
    val fat_100g: Double? = null
)

class OpenFoodFactsClient{

    // HTTP client used to communicate with the OpenFoodFacts API.
    private val client = HttpClient(CIO){
        install(ContentNegotiation){
            json(Json{ignoreUnknownKeys = true})
        }
    }

    // Looks up a food product using its barcode.
    suspend fun lookupBarcode(barcode: String): FoodItemResponse?{
        val response: OffProductResponse = client
            .get("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .body()

        // Return null when the product cannot be found.
        if(response.status == 0 || response.product == null){
            return null
        }

        val product = response.product
        val nutriments = product.nutriments ?: return null

        // Convert nutrition values from 100g to the product's serving size.
        val servingSizeG = product.serving_quantity?.takeIf {it > 0} ?: 100.0
        val scale = servingSizeG/ 100.0

        return FoodItemResponse(
            foodId = barcode,
            name = product.product_name ?: "Unknown product",
            barcode = barcode,
            servingSizeG = servingSizeG,
            calories = (nutriments.`energy-kcal_100g` ?: 0.0) * scale,
            proteinG = (nutriments.proteins_100g ?: 0.0) * scale,
            carbsG = (nutriments.carbohydrates_100g ?: 0.0) * scale,
            fatG = (nutriments.fat_100g ?: 0.0) * scale,
            source = "OpenFoodFacts"
        )
    }

    // Closes the HTTP client when it is no longer needed.
    fun close() = client.close()
}

// ------------------ END OF FILE ------------------ //