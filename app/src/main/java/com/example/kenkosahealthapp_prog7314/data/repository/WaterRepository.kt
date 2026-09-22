package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateWaterRequest
import com.example.kenkosahealthapp_prog7314.data.network.WaterApi
import com.example.kenkosahealthapp_prog7314.data.network.WaterIntakeDto
import retrofit2.HttpException
import java.io.IOException

class WaterRepository(private val api: WaterApi = RetrofitInstance.waterApi) {

    suspend fun getTodaysWater(): ApiResult<WaterIntakeDto> =
        safeCall { api.getTodaysWater() }

    suspend fun updateWater(cupsConsumed: Int): ApiResult<WaterIntakeDto> {
        if (cupsConsumed < 0) return ApiResult.ValidationError("Cups consumed cannot be negative")
        return safeCall { api.updateWater(UpdateWaterRequest(cupsConsumed)) }
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