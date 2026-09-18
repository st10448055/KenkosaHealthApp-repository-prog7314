package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.*
import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ValidationError(val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : ApiResult<Nothing>()
}

class GoalRepository(private val api: GoalApi = RetrofitInstance.goalApi) {

    suspend fun getGoals(): ApiResult<List<GoalDto>> = safeCall { api.getGoals() }

    suspend fun createGoal(
        category: String,
        name: String,
        targetValue: Double,
        unit: String,
        frequency: String,
        remindersEnabled: Boolean
    ): ApiResult<GoalDto> {
        validateGoalInput(name, targetValue, unit)?.let { return ApiResult.ValidationError(it) }

        return safeCall {
            api.createGoal(
                CreateGoalRequest(
                    category = category,
                    name = name.trim(),
                    targetValue = targetValue,
                    unit = unit.trim(),
                    frequency = frequency,
                    remindersEnabled = remindersEnabled
                )
            )
        }
    }

    suspend fun updateGoalProgress(goalId: String, currentValue: Double): ApiResult<GoalDto> {
        if (currentValue < 0) return ApiResult.ValidationError("Progress cannot be negative")
        return safeCall { api.updateGoal(goalId, UpdateGoalRequest(currentValue = currentValue)) }
    }

    suspend fun deleteGoal(goalId: String): ApiResult<Unit> = safeCall { api.deleteGoal(goalId) }

    private fun validateGoalInput(name: String, targetValue: Double, unit: String): String? = when {
        name.isBlank() -> "Goal name cannot be empty"
        name.length > 60 -> "Goal name must be 60 characters or fewer"
        targetValue <= 0 -> "Target value must be greater than 0"
        targetValue > 1_000_000 -> "Target value is unrealistically large"
        unit.isBlank() -> "Unit is required"
        else -> null
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string() ?: e.message()
        ApiResult.ServerError(e.code(), message ?: "Server error")
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }
}