package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SaveSleepLogRequest
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogApi
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import retrofit2.HttpException
import java.io.IOException

/** Own result type, so this feature doesn't depend on (or clash with) any other repository file. */
sealed class SleepLogResult<out T> {
    data class Success<T>(val data: T) : SleepLogResult<T>()
    data class NetworkError(val message: String) : SleepLogResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : SleepLogResult<Nothing>()
}

class SleepLogRepository(private val api: SleepLogApi = RetrofitInstance.sleepLogApi) {

    suspend fun getSleep(): SleepLogResult<List<SleepLogDto>> = safeCall { api.getSleep() }

    suspend fun getSleepRecord(sleepId: String): SleepLogResult<SleepLogDto> =
        safeCall { api.getSleepRecord(sleepId) }

    suspend fun createSleep(request: SaveSleepLogRequest): SleepLogResult<SleepLogDto> =
        safeCall { api.createSleep(request) }

    suspend fun updateSleep(sleepId: String, request: SaveSleepLogRequest): SleepLogResult<SleepLogDto> =
        safeCall { api.updateSleep(sleepId, request) }

    suspend fun deleteSleep(sleepId: String): SleepLogResult<Unit> = safeCall { api.deleteSleep(sleepId) }

    private suspend fun <T> safeCall(block: suspend () -> T): SleepLogResult<T> = try {
        SleepLogResult.Success(block())
    } catch (e: HttpException) {
        // The backend answers errors as {"errors":["..."]}; show those messages, never a blank string.
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = runCatching {
            val errors = org.json.JSONObject(body).getJSONArray("errors")
            (0 until errors.length()).joinToString("\n") { errors.getString(it) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: body.ifBlank { "Request failed (HTTP ${e.code()})" }
        SleepLogResult.ServerError(e.code(), message)
    } catch (e: IOException) {
        SleepLogResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        SleepLogResult.ServerError(0, e.message ?: "Unknown error")
    }
}
