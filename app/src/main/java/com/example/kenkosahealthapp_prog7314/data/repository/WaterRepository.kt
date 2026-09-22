package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateWaterRequest
import com.example.kenkosahealthapp_prog7314.data.network.WaterApi
import com.example.kenkosahealthapp_prog7314.data.network.WaterIntakeDto
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for managing the user's daily water intake.
// It communicates with the backend through WaterApi.
class WaterRepository(
    // Uses the shared Retrofit WaterApi instance by default.
    private val api: WaterApi = RetrofitInstance.waterApi
) {

    // Retrieves the user's water intake for the current day.
    suspend fun getTodaysWater(): ApiResult<WaterIntakeDto> =
        safeCall {
            api.getTodaysWater()
        }

    // Updates the number of cups consumed by the user.
    suspend fun updateWater(
        cupsConsumed: Int
    ): ApiResult<WaterIntakeDto> {

        // Prevents negative water intake values from being sent
        // to the backend.
        if (cupsConsumed < 0) {
            return ApiResult.ValidationError(
                "Cups consumed cannot be negative"
            )
        }

        // Sends the updated water intake to the backend.
        return safeCall {
            api.updateWater(
                UpdateWaterRequest(cupsConsumed)
            )
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
        val message = e.response()?.errorBody()?.string()
            ?: e.message()

        // Returns the HTTP status code and server error message.
        ApiResult.ServerError(
            e.code(),
            message ?: "Server error"
        )

    } catch (e: IOException) {

        // Handles network connectivity problems.
        ApiResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles any other unexpected errors.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //