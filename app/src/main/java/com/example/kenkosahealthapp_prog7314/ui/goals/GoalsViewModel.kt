package com.example.kenkosahealthapp_prog7314.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.GoalDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GoalsUiState {
    object Loading : GoalsUiState()
    data class Success(val goals: List<GoalDto>) : GoalsUiState()
    data class Error(val message: String) : GoalsUiState()
}

class GoalsViewModel(private val repository: GoalRepository = GoalRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
    val uiState: StateFlow<GoalsUiState> = _uiState

    init { loadGoals() }

    fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = GoalsUiState.Loading
            when (val result = repository.getGoals()) {
                is ApiResult.Success -> _uiState.value = GoalsUiState.Success(result.data)
                is ApiResult.NetworkError -> _uiState.value = GoalsUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = GoalsUiState.Error(result.message)
                is ApiResult.ValidationError -> _uiState.value = GoalsUiState.Error(result.message)
            }
        }
    }

    fun createGoal(
        category: String, name: String, targetValue: Double,
        unit: String, frequency: String, remindersEnabled: Boolean,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = repository.createGoal(category, name, targetValue, unit, frequency, remindersEnabled)) {
                is ApiResult.Success -> {
                    loadGoals()
                    onResult(true, null)
                }
                is ApiResult.ValidationError -> onResult(false, result.message)
                is ApiResult.NetworkError -> onResult(false, result.message)
                is ApiResult.ServerError -> onResult(false, result.message)
            }
        }
    }

    fun updateProgress(goalId: String, currentValue: Double) {
        viewModelScope.launch {
            repository.updateGoalProgress(goalId, currentValue)
            loadGoals()
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
            loadGoals()
        }
    }
}