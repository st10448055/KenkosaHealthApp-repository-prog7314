package com.example.kenkosahealthapp_prog7314.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.DiaryRepository
import com.example.kenkosahealthapp_prog7314.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// Represents the state of a food lookup (by text search or barcode scan)
sealed class FoodSearchState{
    object Idle : FoodSearchState()
    object Loading : FoodSearchState()
    data class Found(val food: FoodItemDto) : FoodSearchState()
    data class Error(val message: String) : FoodSearchState()
}

// Represents the state of saving the currently found food as a diary meal entry
sealed class LogMealState {
    object Idle : LogMealState()
    object Saving : LogMealState()
    data class Success(val entry: MealEntryDto) : LogMealState()
    data class Error(val message: String) : LogMealState()
}

// ViewModel backing LogFoodFragment - handles food search (text/barcode) and
// logging the selected food with a chosen meal type and serving size.
class LogFoodViewModel(
    private val foodRepository: FoodRepository = FoodRepository(),
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _searchState = MutableStateFlow<FoodSearchState>(FoodSearchState.Idle)
    val searchState: StateFlow<FoodSearchState> = _searchState

    private val _logState = MutableStateFlow<LogMealState>(LogMealState.Idle)
    val logState: StateFlow<LogMealState> = _logState

    // Currently selected meal type and serving size, defaults chosen for a fresh screen
    var selectedMealType: MealType = MealType.BREAKFAST
        private set
    var selectedServings: Double = 1.0
        private set

    fun selectMealType(mealType: MealType) {
        selectedMealType = mealType
    }

    fun updateServings(servings: Double) {
        selectedServings = servings
    }

    // Looks up a food item by scanned barcode value
    fun searchByBarcode(barcode: String) {
        viewModelScope.launch {
            _searchState.value = FoodSearchState.Loading
            when (val result = foodRepository.searchByBarcode(barcode)) {
                is ApiResult.Success -> _searchState.value = FoodSearchState.Found(result.data)
                is ApiResult.ValidationError -> _searchState.value =
                    FoodSearchState.Error(result.message)

                is ApiResult.NetworkError -> _searchState.value =
                    FoodSearchState.Error(result.message)

                is ApiResult.ServerError -> _searchState.value =
                    FoodSearchState.Error(result.message)
            }
        }
    }

    // Looks up a food item by free-text search query
    fun searchByText(query: String) {
        viewModelScope.launch {
            _searchState.value = FoodSearchState.Loading
            when (val result = foodRepository.searchByQuery(query)) {
                is ApiResult.Success -> _searchState.value = FoodSearchState.Found(result.data)
                is ApiResult.ValidationError -> _searchState.value =
                    FoodSearchState.Error(result.message)

                is ApiResult.NetworkError -> _searchState.value =
                    FoodSearchState.Error(result.message)

                is ApiResult.ServerError -> _searchState.value =
                    FoodSearchState.Error(result.message)
            }
        }
    }

    // Saves the currently found food as a meal entry for today, using the selected meal type and servings
    fun logCurrentFood() {
        val currentFood = (_searchState.value as? FoodSearchState.Found)?.food ?: run {
            _logState.value = LogMealState.Error("No food selected")
            return
        }

        viewModelScope.launch {
            _logState.value = LogMealState.Saving

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            when (val result = diaryRepository.logMeal(
                food = currentFood,
                mealType = selectedMealType.apiValue,
                servings = selectedServings,
                date = today
            )) {
                is ApiResult.Success -> _logState.value = LogMealState.Success(result.data)
                is ApiResult.ValidationError -> _logState.value = LogMealState.Error(result.message)
                is ApiResult.NetworkError -> _logState.value = LogMealState.Error(result.message)
                is ApiResult.ServerError -> _logState.value = LogMealState.Error(result.message)
            }
        }
    }

    // Clears the search result, e.g. when starting a fresh search
    fun resetSearch() {
        _searchState.value = FoodSearchState.Idle
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------