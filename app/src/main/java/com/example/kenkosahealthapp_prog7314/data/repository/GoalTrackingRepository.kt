package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.GoalTrackingApi
import com.example.kenkosahealthapp_prog7314.data.network.LogProgressRequest
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SaveTrackedGoalRequest
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import retrofit2.HttpException
import java.io.IOException

// Common result type used by the goal tracking repository.
// It represents successful requests, validation errors,
// network errors and backend/server errors.
sealed class TrackingResult<out T> {

    // Represents a successful API operation and contains the returned data.
    data class Success<T>(
        val data: T
    ) : TrackingResult<T>()

    // Represents an error caused by invalid input.
    data class ValidationError(
        val message: String
    ) : TrackingResult<Nothing>()

    // Represents a network connectivity error.
    data class NetworkError(
        val message: String
    ) : TrackingResult<Nothing>()

    // Represents an error returned by the backend.
    data class ServerError(
        val code: Int,
        val message: String
    ) : TrackingResult<Nothing>()
}

// Repository responsible for managing tracked health goals.
// It communicates with the backend through GoalTrackingApi.
class GoalTrackingRepository(
    // Uses the shared Retrofit GoalTrackingApi instance by default.
    private val api: GoalTrackingApi = RetrofitInstance.goalTrackingApi
) {

    // Retrieves all tracked goals belonging to the authenticated user.
    suspend fun getGoals(): TrackingResult<List<TrackedGoalDto>> =
        safeCall {
            api.getGoals()
        }

    // Retrieves a specific goal using its unique goal ID.
    suspend fun getGoal(
        goalId: String
    ): TrackingResult<TrackedGoalDto> =
        safeCall {
            api.getGoal(goalId)
        }

    // Creates a new tracked goal using the supplied request.
    suspend fun createGoal(
        request: SaveTrackedGoalRequest
    ): TrackingResult<TrackedGoalDto> =
        safeCall {
            api.createGoal(request)
        }

    // Updates an existing tracked goal.
    suspend fun updateGoal(
        // ID of the goal being updated.
        goalId: String,

        // Updated goal information.
        request: SaveTrackedGoalRequest
    ): TrackingResult<TrackedGoalDto> =
        safeCall {
            api.updateGoal(
                goalId,
                request
            )
        }

    // Records progress for a tracked goal on a specific date.
    suspend fun logProgress(
        // ID of the goal receiving the progress entry.
        goalId: String,

        // Date associated with the progress entry.
        date: String,

        // Amount of progress recorded.
        value: Double
    ): TrackingResult<TrackedGoalDto> {

        // Prevents negative progress values from being sent to the backend.
        if (value < 0) {
            return TrackingResult.ValidationError(
                "Progress cannot be negative"
            )
        }

        // Sends the progress information to the backend.
        return safeCall {
            api.logProgress(
                goalId,
                LogProgressRequest(date, value)
            )
        }
    }

    // Deletes a tracked goal using its unique goal ID.
    suspend fun deleteGoal(
        goalId: String
    ): TrackingResult<Unit> =
        safeCall {
            api.deleteGoal(goalId)
        }

    // Executes an API operation safely and converts exceptions
    // into the appropriate TrackingResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): TrackingResult<T> = try {

        // API request completed successfully.
        TrackingResult.Success(block())

    } catch (e: HttpException) {

        // Retrieves the error response returned by the backend.
        val body = e.response()?.errorBody()?.string().orEmpty()

        // Attempts to extract the backend's "errors" array.
        val message = runCatching {

            val errors = org.json.JSONObject(body)
                .getJSONArray("errors")

            // Combines multiple backend errors into one readable message.
            (0 until errors.length()).joinToString("\n") {
                errors.getString(it)
            }

        }.getOrNull()?.takeIf {
            it.isNotBlank()
        }

        // If the errors array cannot be read, use the response body.
            ?: body.ifBlank {
                "Request failed (HTTP ${e.code()})"
            }

        // Returns the HTTP status code and readable server error message.
        TrackingResult.ServerError(
            e.code(),
            message
        )

    } catch (e: IOException) {

        // Handles network connectivity problems.
        TrackingResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles unexpected errors.
        TrackingResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //