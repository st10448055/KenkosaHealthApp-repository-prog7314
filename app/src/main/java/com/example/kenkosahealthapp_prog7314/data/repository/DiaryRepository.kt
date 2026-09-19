package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMealEntryRequest
import com.example.kenkosahealthapp_prog7314.data.network.DiaryApi
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException


class DiaryRepository(private val api: DiaryApi = RetrofitInstance.diaryApi) {
    suspend fun getMeals(date: String? = null): ApiResult<List<MealEntryDto>> =
        safeCall { api.getMeals(date) }

    suspend fun logMeal(
        food: FoodItemDto,
        mealType: String,
        servings: Double,
        date: String
    ): ApiResult<MealEntryDto> {
        if (servings <= 0) return ApiResult.ValidationError("Servings must be greater than 0")
        if (mealType !in listOf("breakfast", "lunch", "dinner", "snack")) {
            return ApiResult.ValidationError("Invalid meal type")
        }

        return safeCall {
            api.createMeal(
                CreateMealEntryRequest(
                    date = date,
                    mealType = mealType,
                    foodName = food.name,
                    servings = servings,
                    baseCalories = food.calories,
                    baseProteinG = food.proteinG,
                    baseCarbsG = food.carbsG,
                    baseFatG = food.fatG
                )
            )
        }
    }

    suspend fun deleteMeal(mealId: String): ApiResult<Unit> =
        safeCall { api.deleteMeal(mealId) }

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
