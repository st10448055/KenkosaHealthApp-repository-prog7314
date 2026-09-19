package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.FoodApi
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class FoodRepository( private val api: FoodApi = RetrofitInstance.foodApi)
{

    suspend fun searchByBarcode(barcode: String): ApiResult<FoodItemDto> {
        if (barcode.isBlank()) return ApiResult.ValidationError("Barcode cannot be empty")
        return safeCall { api.searchByBarcode(barcode) }
    }

    suspend fun searchByQuery(query: String): ApiResult<FoodItemDto> {
        if (query.isBlank()) return ApiResult.ValidationError("Please enter a food name to search")
        return safeCall { api.searchByQuery(query) }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        if (e.code() == 404) {
            ApiResult.ServerError(404, "No food found — try a different search term or manual entry")
        } else {
            val message = e.response()?.errorBody()?.string() ?: e.message()
            ApiResult.ServerError(e.code(), message ?: "Server error")
        }
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }
}