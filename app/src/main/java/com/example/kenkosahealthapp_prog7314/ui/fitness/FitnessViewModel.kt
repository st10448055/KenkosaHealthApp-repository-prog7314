package com.example.kenkosahealthapp_prog7314.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseCategoryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class FitnessUiState {
    object Loading : FitnessUiState()
    data class Success(val categories: List<ExerciseCategoryDto>) : FitnessUiState()
    data class Error(val message: String) : FitnessUiState()
}

class FitnessViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FitnessUiState>(FitnessUiState.Loading)
    val uiState: StateFlow<FitnessUiState> = _uiState

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading
            when (val result = workoutRepository.getCategories()) {
                is ApiResult.Success -> _uiState.value = FitnessUiState.Success(result.data)
                is ApiResult.ValidationError -> _uiState.value = FitnessUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = FitnessUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = FitnessUiState.Error(result.message)
            }
        }
    }
}