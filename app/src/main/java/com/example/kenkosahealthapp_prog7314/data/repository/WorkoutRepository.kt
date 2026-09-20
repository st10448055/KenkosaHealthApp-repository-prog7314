package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.ExerciseCategoryDto
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.WorkoutApi
import retrofit2.HttpException
import java.io.IOException

class WorkoutRepository(private val api: WorkoutApi = RetrofitInstance.workoutApi) {

    suspend fun getCategories(): ApiResult<List<ExerciseCategoryDto>> =
        safeCall { api.getCategories() }

    suspend fun getExercisesByCategory(categoryId: Int): ApiResult<List<ExerciseDto>> =
        safeCall { api.getExercisesByCategory(categoryId) }

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