package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.GET
import retrofit2.http.Query

// Defines the Retrofit endpoints used to search for food items.
interface FoodApi {

    // Sends a GET request to /api/food/search.
    // Searches for a food item using its barcode.
    // The barcode is added to the request as a query parameter:
    // /api/food/search?barcode=<barcode>
    @GET("api/food/search")
    suspend fun searchByBarcode(
        @Query("barcode") barcode: String
    ): FoodItemDto

    // Sends a GET request to /api/food/search.
    // Searches for a food item using a text-based query.
    // The search text is added to the request as a query parameter:
    // /api/food/search?query=<search text>
    @GET("api/food/search")
    suspend fun searchByQuery(
        @Query("query") query: String
    ): FoodItemDto
}

// ------------------ END OF FILE ------------------ //