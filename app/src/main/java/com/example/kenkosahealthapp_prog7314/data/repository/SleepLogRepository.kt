package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SaveSleepLogRequest
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogApi
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import retrofit2.HttpException
import java.io.IOException

// Custom result type used by the sleep log feature.
// This keeps sleep-related API results separate from other repository result types.
sealed class SleepLogResult<out T> {

    // Represents a successful API operation.
    data class Success<T>(
        val data: T
    ) : SleepLogResult<T>()

    // Represents a network connectivity error.
    data class NetworkError(
        val message: String
    ) : SleepLogResult<Nothing>()

    // Represents an error returned by the backend.
    data class ServerError(
        val code: Int,
        val message: String
    ) : SleepLogResult<Nothing>()
}

// Repository responsible for managing sleep records.
// It communicates with the backend through SleepLogApi.
class SleepLogRepository(
    // Uses the shared Retrofit SleepLogApi instance by default.
    private val api: SleepLogApi = RetrofitInstance.sleepLogApi
) {

    // Retrieves all sleep records belonging to the authenticated user.
    suspend fun getSleep(): SleepLogResult<List<SleepLogDto>> =
        safeCall {
            api.getSleep()
        }

    // Retrieves one specific sleep record using its unique ID.
    suspend fun getSleepRecord(
        sleepId: String
    ): SleepLogResult<SleepLogDto> =
        safeCall {
            api.getSleepRecord(sleepId)
        }

    // Creates a new sleep record using the supplied request.
    suspend fun createSleep(
        request: SaveSleepLogRequest
    ): SleepLogResult<SleepLogDto> =
        safeCall {
            api.createSleep(request)
        }

    // Updates an existing sleep record.
    suspend fun updateSleep(
        // ID of the sleep record being updated.
        sleepId: String,

        // Updated sleep information.
        request: SaveSleepLogRequest
    ): SleepLogResult<SleepLogDto> =
        safeCall {
            api.updateSleep(
                sleepId,
                request
            )
        }

    // Deletes an existing sleep record using its unique ID.
    suspend fun deleteSleep(
        sleepId: String
    ): SleepLogResult<Unit> =
        safeCall {
            api.deleteSleep(sleepId)
        }

    // Executes an API operation safely and converts exceptions
    // into the appropriate SleepLogResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): SleepLogResult<T> = try {

        // API request completed successfully.
        SleepLogResult.Success(block())

    } catch (e: HttpException) {

        // The backend returns errors using an "errors" JSON array.
        // Extract those messages instead of showing a blank error.
        val body = e.response()?.errorBody()?.string().orEmpty()

        // Attempts to extract all messages from the backend errors array.
        val message = runCatching {

            val errors = org.json.JSONObject(body)
                .getJSONArray("errors")

            // Combines multiple errors into one readable message.
            (0 until errors.length()).joinToString("\n") {
                errors.getString(it)
            }

        }.getOrNull()?.takeIf {
            it.isNotBlank()
        }

        // Uses the response body if an errors array is unavailable.
            ?: body.ifBlank {
                "Request failed (HTTP ${e.code()})"
            }

        // Returns the HTTP status code and extracted error message.
        SleepLogResult.ServerError(
            e.code(),
            message
        )

    } catch (e: IOException) {

        // Handles network connectivity problems.
        SleepLogResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles unexpected errors.
        SleepLogResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //