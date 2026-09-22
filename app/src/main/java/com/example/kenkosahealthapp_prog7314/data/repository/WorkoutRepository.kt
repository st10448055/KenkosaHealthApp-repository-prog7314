package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.ExerciseCategoryDto
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.WorkoutApi
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for retrieving workout and exercise information.
// It acts as a middle layer between the application and the WorkoutApi.
class WorkoutRepository(
    // Uses the shared Retrofit WorkoutApi instance by default.
    private val api: WorkoutApi = RetrofitInstance.workoutApi
) {

    // Retrieves all available exercise categories from the backend.
    suspend fun getCategories(): ApiResult<List<ExerciseCategoryDto>> =
        safeCall {
            api.getCategories()
        }

    // Retrieves all exercises belonging to the selected category.
    suspend fun getExercisesByCategory(
        categoryId: Int
    ): ApiResult<List<ExerciseDto>> =
        safeCall {
            api.getExercisesByCategory(categoryId)
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