package com.example.kenkosahealthapp_prog7314.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// State of the exercise list for a single category.
sealed class ExerciseListUiState {
    object Loading : ExerciseListUiState()

    // Keeps both the full unfiltered list and the currently filtered/displayed
    // list, so search can be reapplied without hitting the network again.
    data class Success(val allExercises: List<ExerciseDto>, val filtered: List<ExerciseDto>) : ExerciseListUiState()
    data class Error(val message: String) : ExerciseListUiState()
}

// Backs ExerciseListFragment: loads exercises for a given category and
// supports client-side search filtering over the already-loaded results.
class ExerciseListViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExerciseListUiState>(ExerciseListUiState.Loading)
    val uiState: StateFlow<ExerciseListUiState> = _uiState

    // Fetches all exercises belonging to the given category from the server.
    fun loadExercises(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = ExerciseListUiState.Loading
            when (val result = workoutRepository.getExercisesByCategory(categoryId)) {
                // Initially, filtered == allExercises since no search query has been entered yet.
                is ApiResult.Success -> _uiState.value =
                    ExerciseListUiState.Success(allExercises = result.data, filtered = result.data)
                is ApiResult.ValidationError -> _uiState.value = ExerciseListUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = ExerciseListUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = ExerciseListUiState.Error(result.message)
            }
        }
    }

    // Client-side filtering,
    fun search(query: String) {
        // No-op if we're not currently in a loaded/success state (e.g. still loading or errored).
        val current = _uiState.value as? ExerciseListUiState.Success ?: return

        // Blank query shows everything again; otherwise filter by case-insensitive name match.
        val filtered = if (query.isBlank()) {
            current.allExercises
        } else {
            current.allExercises.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Keep allExercises untouched, only update what's displayed.
        _uiState.value = current.copy(filtered = filtered)
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------