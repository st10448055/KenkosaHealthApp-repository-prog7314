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

data class MealHistoryUiState(
    val isLoading: Boolean = false,
    val breakfast: List<MealEntryDto> = emptyList(),
    val lunch: List<MealEntryDto> = emptyList(),
    val dinner: List<MealEntryDto> = emptyList(),
    val snack: List<MealEntryDto> = emptyList(),
    val errorMessage: String? = null
)

class MealHistoryViewModel(
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealHistoryUiState())
    val uiState: StateFlow<MealHistoryUiState> = _uiState

    init {
        loadTodaysMeals()
    }

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

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            when (diaryRepository.deleteMeal(mealId)) {
                is ApiResult.Success -> loadTodaysMeals()
                else -> Unit
            }
        }
    }
}