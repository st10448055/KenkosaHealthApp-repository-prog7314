package com.example.kenkosahealthapp_prog7314.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.SaveTrackedGoalRequest
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.data.repository.GoalTrackingRepository
import com.example.kenkosahealthapp_prog7314.data.repository.TrackingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// UI state representations for goal tracking list screen
sealed class GoalTrackingUiState {
    object Loading : GoalTrackingUiState()
    data class Success(val goals: List<TrackedGoalDto>) : GoalTrackingUiState()
    data class Error(val message: String) : GoalTrackingUiState()
}


// UI state representations for adding/editing a single goal
sealed class GoalEditState {
    object Idle : GoalEditState()
    object Loading : GoalEditState()
    data class Loaded(val goal: TrackedGoalDto) : GoalEditState()
    data class Error(val message: String) : GoalEditState()
}

// ViewModel managing states and operations for goal tracking screens
class GoalTrackingViewModel(
    private val repository: GoalTrackingRepository = GoalTrackingRepository()
) : ViewModel() {

    // Internal state flow for goal list screen
    private val _uiState = MutableStateFlow<GoalTrackingUiState>(GoalTrackingUiState.Loading)

    // Public state flow for goal list screen
    val uiState: StateFlow<GoalTrackingUiState> = _uiState

    // Internal state flow for add/edit goal screen
    private val _editState = MutableStateFlow<GoalEditState>(GoalEditState.Idle)

    // Public state flow for add/edit goal screen
    val editState: StateFlow<GoalEditState> = _editState

    // ------------------------------------------------------------------ list

    fun loadGoals() {
        viewModelScope.launch {
            // Only show the spinner on the very first load so returning to the screen doesn't flicker.
            if (_uiState.value !is GoalTrackingUiState.Success) _uiState.value = GoalTrackingUiState.Loading
            _uiState.value = when (val result = repository.getGoals()) {
                is TrackingResult.Success -> GoalTrackingUiState.Success(result.data)
                else -> GoalTrackingUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    // Logs progress amount for target date and refreshes goal list
    fun logProgress(
        goalId: String, date: String, value: Double,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.logProgress(goalId, date, value)
            onResult(result is TrackingResult.Success, result.errorMessage())
            loadGoals()
        }
    }

    // Deletes specified goal and refreshes goal list
    fun deleteGoal(
        goalId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteGoal(goalId)
            onResult?.invoke(result is TrackingResult.Success, result.errorMessage())
            loadGoals()
        }
    }

    // ------------------------------------------------------------- add / edit

    // Loads single goal entity for editing
    fun loadGoal(goalId: String) {
        viewModelScope.launch {
            _editState.value = GoalEditState.Loading
            _editState.value = when (val result = repository.getGoal(goalId)) {
                is TrackingResult.Success -> GoalEditState.Loaded(result.data)
                else -> GoalEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    // Creates new goal entry
    fun createGoal(
        request: SaveTrackedGoalRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createGoal(request)
            onResult(result is TrackingResult.Success, result.errorMessage())
        }
    }

    // Updates existing goal entry
    fun updateGoal(
        goalId: String, request: SaveTrackedGoalRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateGoal(goalId, request)
            onResult(result is TrackingResult.Success, result.errorMessage())
        }
    }

    // Extracts error message string from repository result
    private fun TrackingResult<*>.errorMessage(): String? = when (this) {
        is TrackingResult.Success -> null
        is TrackingResult.ValidationError -> message
        is TrackingResult.NetworkError -> message
        is TrackingResult.NetworkError -> message
        is TrackingResult.ServerError -> message
    }
}
//---------------------------------------END OF FILE-------------------------------------------------