package com.example.kenkosahealthapp_prog7314.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.WaterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WaterUiState {
    object Loading : WaterUiState()
    data class Success(val cupsConsumed: Int, val targetCups: Int) : WaterUiState()
    data class Error(val message: String) : WaterUiState()
}

class WaterViewModel(
    private val waterRepository: WaterRepository = WaterRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WaterUiState>(WaterUiState.Loading)
    val uiState: StateFlow<WaterUiState> = _uiState

    init {
        loadWater()
    }

    fun loadWater() {
        viewModelScope.launch {
            _uiState.value = WaterUiState.Loading
            when (val result = waterRepository.getTodaysWater()) {
                is ApiResult.Success -> _uiState.value =
                    WaterUiState.Success(result.data.cupsConsumed, result.data.targetCups)
                is ApiResult.ValidationError -> _uiState.value = WaterUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = WaterUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = WaterUiState.Error(result.message)
            }
        }
    }

    /** Called when the user taps a specific cup — sets today's total to that cup's position. */
    fun setCups(cupNumber: Int) {
        val current = _uiState.value as? WaterUiState.Success ?: return

        // Tapping the currently-filled cup again empties it (toggle behaviour),
        // matching the "click to fill/unfill" pattern you described.
        val newValue = if (cupNumber == current.cupsConsumed) cupNumber - 1 else cupNumber

        viewModelScope.launch {
            when (val result = waterRepository.updateWater(newValue)) {
                is ApiResult.Success -> _uiState.value =
                    WaterUiState.Success(result.data.cupsConsumed, result.data.targetCups)
                is ApiResult.ValidationError -> _uiState.value = WaterUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = WaterUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = WaterUiState.Error(result.message)
            }
        }
    }
}