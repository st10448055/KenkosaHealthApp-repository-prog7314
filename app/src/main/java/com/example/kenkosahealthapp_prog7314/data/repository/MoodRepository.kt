package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMoodRequest
import com.example.kenkosahealthapp_prog7314.data.network.MoodApi
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMoodRequest
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for managing mood data.
// It acts as the middle layer between the application and the MoodApi.
class MoodRepository(
    // Uses the shared Retrofit MoodApi instance by default.
    private val api: MoodApi = RetrofitInstance.moodApi
) {

    // Retrieves all mood entries belonging to the authenticated user.
    suspend fun getMoods(): ApiResult<List<MoodEntryDto>> =
        safeCall { api.getMoods() }

    // Retrieves a specific mood entry using its unique mood ID.
    suspend fun getMood(moodId: String): ApiResult<MoodEntryDto> =
        safeCall { api.getMood(moodId) }

    // Creates a new mood entry.
    suspend fun createMood(
        // Main mood selected by the user.
        mood: String,

        // Additional feelings selected by the user.
        feelings: List<String>,

        // Time when the mood was logged, stored as epoch milliseconds.
        loggedAt: Long
    ): ApiResult<MoodEntryDto> =
        safeCall {
            // Creates the request object and sends it to the backend.
            api.createMood(
                CreateMoodRequest(
                    mood,
                    feelings,
                    loggedAt
                )
            )
        }

    // Updates an existing mood entry using its unique ID.
    suspend fun updateMood(
        // ID of the mood entry to update.
        moodId: String,

        // Updated main mood.
        mood: String,

        // Updated list of feelings.
        feelings: List<String>,

        // Updated mood logging time.
        loggedAt: Long
    ): ApiResult<MoodEntryDto> =
        safeCall {
            // Creates the update request and sends it to the backend.
            api.updateMood(
                moodId,
                UpdateMoodRequest(
                    mood,
                    feelings,
                    loggedAt
                )
            )
        }

    // Deletes an existing mood entry using its unique ID.
    suspend fun deleteMood(
        moodId: String
    ): ApiResult<Unit> =
        safeCall {
            api.deleteMood(moodId)
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

        // Handles network problems such as the server being unreachable
        // or the device having no internet connection.
        ApiResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles any other unexpected error.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //