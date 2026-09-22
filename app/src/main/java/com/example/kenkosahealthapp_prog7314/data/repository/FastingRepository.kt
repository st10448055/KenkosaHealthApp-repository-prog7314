package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateFastRequest
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.data.network.FastingApi
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateFastRequest
import retrofit2.HttpException
import java.io.IOException

class FastRepository(private val api: FastingApi = RetrofitInstance.fastingApi) {

    suspend fun getFasts(): ApiResult<List<FastDto>> = safeCall { api.getFasts() }

    suspend fun getFast(fastId: String): ApiResult<FastDto> = safeCall { api.getFast(fastId) }

    suspend fun createFast(startTime: Long, endTime: Long): ApiResult<FastDto> =
        safeCall { api.createFast(CreateFastRequest(startTime, endTime)) }

    /** Only the fields you pass are changed. */
    suspend fun updateFast(
        fastId: String,
        startTime: Long? = null,
        endTime: Long? = null,
        active: Boolean? = null
    ): ApiResult<FastDto> =
        safeCall { api.updateFast(fastId, UpdateFastRequest(startTime, endTime, active)) }

    suspend fun deleteFast(fastId: String): ApiResult<Unit> = safeCall { api.deleteFast(fastId) }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        // The backend answers errors as {"errors":["..."]}; show those messages, never a blank string.
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = runCatching {
            val errors = org.json.JSONObject(body).getJSONArray("errors")
            (0 until errors.length()).joinToString("\n") { errors.getString(it) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: body.ifBlank { "Request failed (HTTP ${e.code()})" }
        ApiResult.ServerError(e.code(), message)
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }
}