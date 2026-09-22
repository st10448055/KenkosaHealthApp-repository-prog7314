package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMoodRequest
import com.example.kenkosahealthapp_prog7314.data.network.MoodApi
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMoodRequest
import retrofit2.HttpException
import java.io.IOException

class MoodRepository(private val api: MoodApi = RetrofitInstance.moodApi) {

    suspend fun getMoods(): ApiResult<List<MoodEntryDto>> = safeCall { api.getMoods() }

    suspend fun getMood(moodId: String): ApiResult<MoodEntryDto> = safeCall { api.getMood(moodId) }

    suspend fun createMood(mood: String, feelings: List<String>, loggedAt: Long): ApiResult<MoodEntryDto> =
        safeCall { api.createMood(CreateMoodRequest(mood, feelings, loggedAt)) }

    suspend fun updateMood(
        moodId: String,
        mood: String,
        feelings: List<String>,
        loggedAt: Long
    ): ApiResult<MoodEntryDto> =
        safeCall { api.updateMood(moodId, UpdateMoodRequest(mood, feelings, loggedAt)) }

    suspend fun deleteMood(moodId: String): ApiResult<Unit> = safeCall { api.deleteMood(moodId) }

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
