package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.GoalTrackingApi
import com.example.kenkosahealthapp_prog7314.data.network.LogProgressRequest
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SaveTrackedGoalRequest
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.util.TimeZone

sealed class TrackingResult<out T> {
    data class Success<T>(val data: T) : TrackingResult<T>()
    data class ValidationError(val message: String) : TrackingResult<Nothing>()
    data class NetworkError(val message: String) : TrackingResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : TrackingResult<Nothing>()
}

class GoalTrackingRepository(private val api: GoalTrackingApi = RetrofitInstance.goalTrackingApi) {

    suspend fun getGoals(): TrackingResult<List<TrackedGoalDto>> = safeCall { api.getGoals() }

    suspend fun getGoal(goalId: String): TrackingResult<TrackedGoalDto> = safeCall { api.getGoal(goalId) }

    suspend fun createGoal(request: SaveTrackedGoalRequest): TrackingResult<TrackedGoalDto> =
        safeCall { api.createGoal(request) }

    suspend fun updateGoal(goalId: String, request: SaveTrackedGoalRequest): TrackingResult<TrackedGoalDto> =
        safeCall { api.updateGoal(goalId, request) }

    suspend fun logProgress(goalId: String, date: String, value: Double): TrackingResult<TrackedGoalDto> {
        if (value < 0) return TrackingResult.ValidationError("Progress cannot be negative")
        return safeCall { api.logProgress(goalId, LogProgressRequest(date, value)) }
    }

    suspend fun deleteGoal(goalId: String): TrackingResult<Unit> = safeCall { api.deleteGoal(goalId) }

    /**
     * Asks the server to fill today's WATER and SLEEP goal progress from the water and sleep the user
     * already logged. Returns the updated goals.
     */
    suspend fun syncGoals(): TrackingResult<List<TrackedGoalDto>> = safeCall {
        RetrofitInstance.statsApi.syncGoals(LocalDate.now().toString(), TimeZone.getDefault().id)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): TrackingResult<T> = try {
        TrackingResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = runCatching {
            val errors = org.json.JSONObject(body).getJSONArray("errors")
            (0 until errors.length()).joinToString("\n") { errors.getString(it) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: body.ifBlank { "Request failed (HTTP ${e.code()})" }
        TrackingResult.ServerError(e.code(), message)
    } catch (e: IOException) {
        TrackingResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        TrackingResult.ServerError(0, e.message ?: "Unknown error")
    }
}