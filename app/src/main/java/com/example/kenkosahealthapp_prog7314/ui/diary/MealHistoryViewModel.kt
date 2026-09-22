package com.example.kenkosahealthapp_prog7314.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Groups today's meal entries by meal type for display, plus loading/error state
data class MealHistoryUiState(
    val isLoading: Boolean = false,
    val breakfast: List<MealEntryDto> = emptyList(),
    val lunch: List<MealEntryDto> = emptyList(),
    val dinner: List<MealEntryDto> = emptyList(),
    val snack: List<MealEntryDto> = emptyList(),
    val errorMessage: String? = null
)

// ViewModel shared by MealHistoryFragment and DiaryFragment - loads today's meals
// grouped by type, and handles updating servings or deleting an entry.
class MealHistoryViewModel(
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealHistoryUiState())
    val uiState: StateFlow<MealHistoryUiState> = _uiState

    init {
        loadTodaysMeals()
    }

    // Fetches today's meals and buckets them into breakfast/lunch/dinner/snack lists
    fun loadTodaysMeals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            when (val result = diaryRepository.getMeals(today)) {
                is ApiResult.Success -> {
                    val meals = result.data
                    _uiState.value = MealHistoryUiState(
                        isLoading = false,
                        breakfast = meals.filter { it.mealType == "breakfast" },
                        lunch = meals.filter { it.mealType == "lunch" },
                        dinner = meals.filter { it.mealType == "dinner" },
                        snack = meals.filter { it.mealType == "snack" }
                    )
                }
                is ApiResult.NetworkError -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is ApiResult.ServerError -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is ApiResult.ValidationError -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
            }
        }
    }

    // Updates a meal entry's serving size, then reloads so totals reflect the change
    fun updateServings(mealId: String, newServings: Double) {
        viewModelScope.launch {
            when (diaryRepository.updateServings(mealId, newServings)) {
                is ApiResult.Success -> loadTodaysMeals() // simplest correct approach: refetch to get accurate recalculated totals
                is ApiResult.ValidationError -> Unit // TODO: surface this to the user if you want inline validation feedback
                is ApiResult.NetworkError -> Unit
                is ApiResult.ServerError -> Unit
            }
        }
    }

    // Deletes a meal entry, then reloads to refresh the lists
    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            when (diaryRepository.deleteMeal(mealId)) {
                is ApiResult.Success -> loadTodaysMeals()
                else -> Unit
            }
        }
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------