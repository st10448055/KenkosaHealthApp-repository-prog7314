package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.CreateMealEntryRequest
import com.example.kenkosahealthapp_prog7314.data.network.DiaryApi
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMealEntryRequest
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for managing diary meal entries.
// It provides the application with methods for retrieving, creating,
// updating and deleting meals while keeping API communication and
// error handling separate from the UI.
class DiaryRepository(
    // Uses the shared Retrofit DiaryApi instance by default.
    private val api: DiaryApi = RetrofitInstance.diaryApi
) {

    // Retrieves meal entries.
    //
    // The date parameter is optional. If supplied, the backend can
    // return meals for that specific date.
    suspend fun getMeals(
        date: String? = null
    ): ApiResult<List<MealEntryDto>> =
        safeCall {
            api.getMeals(date)
        }


    // Logs a food item as a meal in the user's diary.
    // Nutrition values are taken from the selected FoodItemDto
    // and sent to the backend as the base nutritional values.
    suspend fun logMeal(
        // Food item selected by the user.
        food: FoodItemDto,

        // Meal category, such as breakfast, lunch, dinner or snack.
        mealType: String,

        // Number of servings consumed.
        servings: Double,

        // Date on which the meal was consumed.
        date: String
    ): ApiResult<MealEntryDto> {

        // Prevents zero or negative serving amounts.
        if (servings <= 0) {
            return ApiResult.ValidationError(
                "Servings must be greater than 0"
            )
        }

        // Ensures that only supported meal types are submitted.
        if (mealType !in listOf("breakfast", "lunch", "dinner", "snack")) {
            return ApiResult.ValidationError(
                "Invalid meal type"
            )
        }

        // Sends the meal information to the backend.
        return safeCall {
            api.createMeal(
                CreateMealEntryRequest(
                    // Date of the meal.
                    date = date,

                    // Type of meal being logged.
                    mealType = mealType,

                    // Name of the selected food.
                    foodName = food.name,

                    // Number of servings consumed.
                    servings = servings,

                    // Base calories per serving.
                    baseCalories = food.calories,

                    // Base protein per serving.
                    baseProteinG = food.proteinG,

                    // Base carbohydrates per serving.
                    baseCarbsG = food.carbsG,

                    // Base fat per serving.
                    baseFatG = food.fatG
                )
            )
        }
    }

    // Updates the number of servings for an existing meal.
    suspend fun updateServings(
        // ID of the meal to update.
        mealId: String,

        // New number of servings.
        servings: Double
    ): ApiResult<MealEntryDto> {

        // Prevents zero or negative serving amounts.
        if (servings <= 0) {
            return ApiResult.ValidationError(
                "Servings must be greater than 0"
            )
        }

        // Sends the updated serving amount to the backend.
        return safeCall {
            api.updateMeal(
                mealId,
                UpdateMealEntryRequest(servings)
            )
        }
    }

    // Deletes a meal entry using its unique meal ID.
    suspend fun deleteMeal(
        mealId: String
    ): ApiResult<Unit> =
        safeCall {
            api.deleteMeal(mealId)
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

        // Returns the HTTP status code and error message.
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

        // Handles unexpected errors that are not HTTP or network errors.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //