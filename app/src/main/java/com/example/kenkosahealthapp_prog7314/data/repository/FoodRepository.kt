package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.FoodApi
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for searching for food items.
// It communicates with the backend through FoodApi.
class FoodRepository(
    // Uses the shared Retrofit FoodApi instance by default.
    private val api: FoodApi = RetrofitInstance.foodApi
) {

    // Searches for a food item using its barcode.
    suspend fun searchByBarcode(
        barcode: String
    ): ApiResult<FoodItemDto> {

        // Prevents an empty barcode from being sent to the backend.
        if (barcode.isBlank()) {
            return ApiResult.ValidationError(
                "Barcode cannot be empty"
            )
        }

        // Sends the barcode search request to the backend.
        return safeCall {
            api.searchByBarcode(barcode)
        }
    }

    // Searches for a food item using a food name or search query.
    suspend fun searchByQuery(
        query: String
    ): ApiResult<FoodItemDto> {

        // Prevents an empty search query from being sent to the backend.
        if (query.isBlank()) {
            return ApiResult.ValidationError(
                "Please enter a food name to search"
            )
        }

        // Sends the food search query to the backend.
        return safeCall {
            api.searchByQuery(query)
        }
    }

    // Executes an API operation safely and converts exceptions
    // into the appropriate ApiResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): ApiResult<T> = try {

        // API request completed successfully.
        ApiResult.Success(block())

    } catch (e: HttpException) {

        // Handles HTTP errors returned by the backend.
        if (e.code() == 404) {

            // A 404 means that no matching food item was found.
            ApiResult.ServerError(
                404,
                "No food found — try a different search term or manual entry"
            )

        } else {

            // Handles other HTTP errors returned by the backend.
            val message = e.response()?.errorBody()?.string()
                ?: e.message()

            ApiResult.ServerError(
                e.code(),
                message ?: "Server error"
            )
        }

    } catch (e: IOException) {

        // Handles network connectivity problems.
        ApiResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles unexpected errors.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //