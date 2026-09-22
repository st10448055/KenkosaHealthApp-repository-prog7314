package com.example.kenkosahealthapp_prog7314.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.WaterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Represents the different states the water screen can be in.
sealed class WaterUiState {

    // Data is currently being loaded.
    object Loading : WaterUiState()

    // Data was loaded successfully and contains the current progress and daily target.
    data class Success(val cupsConsumed: Int, val targetCups: Int) : WaterUiState()

    // Something went wrong and a message can be shown to the user.
    data class Error(val message: String) : WaterUiState()
}

class WaterViewModel(
    // The repository handles communication with the water data source.
    private val waterRepository: WaterRepository = WaterRepository()
) : ViewModel() {

    // Mutable state is kept private so only the ViewModel can change it.
    private val _uiState = MutableStateFlow<WaterUiState>(WaterUiState.Loading)

    // The Fragment receives a read-only StateFlow to observe changes.
    val uiState: StateFlow<WaterUiState> = _uiState

    init {
        // Load today's water information as soon as the ViewModel is created.
        loadWater()
    }

    fun loadWater() {
        // viewModelScope automatically cancels the coroutine when the ViewModel is cleared.
        viewModelScope.launch {
            _uiState.value = WaterUiState.Loading

            // Request today's water data from the repository.
            when (val result = waterRepository.getTodaysWater()) {

                is ApiResult.Success -> _uiState.value =
                    WaterUiState.Success(
                        result.data.cupsConsumed,
                        result.data.targetCups
                    )

                // Convert different API errors into an Error state for the UI.
                is ApiResult.ValidationError ->
                    _uiState.value = WaterUiState.Error(result.message)

                is ApiResult.NetworkError ->
                    _uiState.value = WaterUiState.Error(result.message)

                is ApiResult.ServerError ->
                    _uiState.value = WaterUiState.Error(result.message)
            }
        }
    }

    // Called when the user taps a specific cup.
    // The selected cup number becomes the new total for today's water intake.
    fun setCups(cupNumber: Int) {

        // Only continue if the current state contains successfully loaded data.
        val current = _uiState.value as? WaterUiState.Success ?: return

        // Tapping the currently-filled cup again removes that cup.
        // Otherwise, the tapped cup becomes the new total.
        val newValue = if (cupNumber == current.cupsConsumed) {
            cupNumber - 1
        } else {
            cupNumber
        }

        // Save the updated total through the repository.
        viewModelScope.launch {
            when (val result = waterRepository.updateWater(newValue)) {

                is ApiResult.Success -> _uiState.value =
                    WaterUiState.Success(
                        result.data.cupsConsumed,
                        result.data.targetCups
                    )

                // Convert any update errors into an Error state for the Fragment.
                is ApiResult.ValidationError ->
                    _uiState.value = WaterUiState.Error(result.message)

                is ApiResult.NetworkError ->
                    _uiState.value = WaterUiState.Error(result.message)

                is ApiResult.ServerError ->
                    _uiState.value = WaterUiState.Error(result.message)
            }
        }
    }
}