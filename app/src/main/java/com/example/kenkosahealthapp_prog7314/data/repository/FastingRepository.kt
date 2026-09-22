package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateFastRequest
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.data.network.FastingApi
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateFastRequest
import retrofit2.HttpException
import java.io.IOException


// Repository responsible for managing fasting data.
// The repository acts as an abstraction layer between the UI/application
// and the FastingApi. It also handles API errors and converts them into
// the common ApiResult format used throughout the application.
class FastRepository(
    // Uses the shared Retrofit FastingApi instance by default.
    private val api: FastingApi = RetrofitInstance.fastingApi
) {

    // Retrieves all fasting records belonging to the authenticated user.
    suspend fun getFasts(): ApiResult<List<FastDto>> =
        safeCall { api.getFasts() }

    // Retrieves a specific fasting record using its unique ID.
    suspend fun getFast(fastId: String): ApiResult<FastDto> =
        safeCall { api.getFast(fastId) }

    // Creates a new fasting record using the supplied start and end times.
    suspend fun createFast(
        startTime: Long,
        endTime: Long
    ): ApiResult<FastDto> =
        safeCall {
            api.createFast(
                CreateFastRequest(startTime, endTime)
            )
        }


    // Updates an existing fasting record.
    // Only the fields that are not null are intended to be changed
    // by the backend.
    suspend fun updateFast(
        // ID of the fasting record to update.
        fastId: String,

        // New start time, or null if it should remain unchanged.
        startTime: Long? = null,

        // New end time, or null if it should remain unchanged.
        endTime: Long? = null,

        // New active status, or null if it should remain unchanged.
        active: Boolean? = null
    ): ApiResult<FastDto> =
        safeCall {
            api.updateFast(
                fastId,
                UpdateFastRequest(startTime, endTime, active)
            )
        }

    // Deletes a fasting record using its unique ID.
    suspend fun deleteFast(fastId: String): ApiResult<Unit> =
        safeCall { api.deleteFast(fastId) }


     // Executes an API operation safely and converts exceptions
     // into the appropriate ApiResult type.
     // This keeps network and server error handling in one place.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): ApiResult<T> = try {

        // API request completed successfully.
        ApiResult.Success(block())

    } catch (e: HttpException) {

        // The backend returned an HTTP error response.
        // The backend answers errors as {"errors":["..."]}.
        // These messages are extracted so the user does not receive
        // a blank or unhelpful error message.
        val body = e.response()?.errorBody()?.string().orEmpty()

        // Attempts to extract all error messages from the backend JSON.
        val message = runCatching {
            val errors = org.json.JSONObject(body).getJSONArray("errors")

            // Combines multiple backend errors into one readable message.
            (0 until errors.length()).joinToString("\n") {
                errors.getString(it)
            }
        }.getOrNull()?.takeIf {
            it.isNotBlank()
        }

        // If the JSON does not contain an errors array,
        // use the raw response body when available.
            ?: body.ifBlank {
                "Request failed (HTTP ${e.code()})"
            }

        // Returns the HTTP status code and readable server message.
        ApiResult.ServerError(e.code(), message)

    } catch (e: IOException) {

        // Handles connection problems such as no internet connection
        // or the server being unreachable.
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