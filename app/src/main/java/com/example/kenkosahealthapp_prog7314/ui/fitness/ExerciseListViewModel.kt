package com.example.kenkosahealthapp_prog7314.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ExerciseListUiState {
    object Loading : ExerciseListUiState()
    data class Success(val allExercises: List<ExerciseDto>, val filtered: List<ExerciseDto>) : ExerciseListUiState()
    data class Error(val message: String) : ExerciseListUiState()
}

class ExerciseListViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExerciseListUiState>(ExerciseListUiState.Loading)
    val uiState: StateFlow<ExerciseListUiState> = _uiState

    fun loadExercises(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = ExerciseListUiState.Loading
            when (val result = workoutRepository.getExercisesByCategory(categoryId)) {
                is ApiResult.Success -> _uiState.value =
                    ExerciseListUiState.Success(allExercises = result.data, filtered = result.data)
                is ApiResult.ValidationError -> _uiState.value = ExerciseListUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = ExerciseListUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = ExerciseListUiState.Error(result.message)
            }
        }
    }

    /** Client-side filtering, */
    fun search(query: String) {
        val current = _uiState.value as? ExerciseListUiState.Success ?: return

        val filtered = if (query.isBlank()) {
            current.allExercises
        } else {
            current.allExercises.filter { it.name.contains(query, ignoreCase = true) }
        }

        _uiState.value = current.copy(filtered = filtered)
    }
}