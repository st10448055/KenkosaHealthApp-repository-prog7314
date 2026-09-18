package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateSleepRequest
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SleepApi
import com.example.kenkosahealthapp_prog7314.data.network.SleepRecordDto
import retrofit2.HttpException
import java.io.IOException

class SleepRepository(private val api: SleepApi = RetrofitInstance.sleepApi) {

    suspend fun getSleep(from: Long? = null, to: Long? = null): ApiResult<List<SleepRecordDto>> =
        safeCall { api.getSleep(from, to) }

    suspend fun createSleep(
        sleepStart: Long,
        sleepEnd: Long,
        qualityRating: Int,
        notes: String?
    ): ApiResult<SleepRecordDto> {
        if (sleepStart <= 0) return ApiResult.ValidationError("Please set a start time")
        if (sleepEnd <= 0) return ApiResult.ValidationError("Please set an end time")
        if (qualityRating !in 1..5) return ApiResult.ValidationError("Please select a sleep quality")
        if (!notes.isNullOrEmpty() && notes.length > 500) {
            return ApiResult.ValidationError("Notes must be 500 characters or fewer")
        }

        return safeCall {
            api.createSleep(
                CreateSleepRequest(
                    sleepStart = sleepStart,
                    sleepEnd = sleepEnd,
                    qualityRating = qualityRating,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
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