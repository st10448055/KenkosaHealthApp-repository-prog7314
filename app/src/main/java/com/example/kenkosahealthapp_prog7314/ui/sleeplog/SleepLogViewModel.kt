package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.SaveSleepLogRequest
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import com.example.kenkosahealthapp_prog7314.data.repository.SleepLogRepository
import com.example.kenkosahealthapp_prog7314.data.repository.SleepLogResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SleepLogUiState {
    object Loading : SleepLogUiState()
    data class Success(val records: List<SleepLogDto>) : SleepLogUiState()
    data class Error(val message: String) : SleepLogUiState()
}

/** State of the single record loaded by the Add / Edit Sleep screen. */
sealed class SleepEditState {
    object Idle : SleepEditState()
    object Loading : SleepEditState()
    data class Loaded(val record: SleepLogDto) : SleepEditState()
    data class Error(val message: String) : SleepEditState()
}

class SleepLogViewModel(
    private val repository: SleepLogRepository = SleepLogRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepLogUiState>(SleepLogUiState.Loading)
    val uiState: StateFlow<SleepLogUiState> = _uiState

    private val _editState = MutableStateFlow<SleepEditState>(SleepEditState.Idle)
    val editState: StateFlow<SleepEditState> = _editState

    // ------------------------------------------------------------------ list

    fun loadSleep() {
        viewModelScope.launch {
            // Only show the spinner on the very first load so returning to the screen doesn't flicker.
            if (_uiState.value !is SleepLogUiState.Success) _uiState.value = SleepLogUiState.Loading
            _uiState.value = when (val result = repository.getSleep()) {
                is SleepLogResult.Success -> SleepLogUiState.Success(result.data)
                else -> SleepLogUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun deleteSleep(
        sleepId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteSleep(sleepId)
            onResult?.invoke(result is SleepLogResult.Success, result.errorMessage())
            loadSleep()
        }
    }

    // ------------------------------------------------------------- add / edit

    fun loadRecord(sleepId: String) {
        viewModelScope.launch {
            _editState.value = SleepEditState.Loading
            _editState.value = when (val result = repository.getSleepRecord(sleepId)) {
                is SleepLogResult.Success -> SleepEditState.Loaded(result.data)
                else -> SleepEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun createSleep(
        request: SaveSleepLogRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createSleep(request)
            onResult(result is SleepLogResult.Success, result.errorMessage())
        }
    }

    fun updateSleep(
        sleepId: String, request: SaveSleepLogRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateSleep(sleepId, request)
            onResult(result is SleepLogResult.Success, result.errorMessage())
        }
    }

    private fun SleepLogResult<*>.errorMessage(): String? = when (this) {
        is SleepLogResult.Success -> null
        is SleepLogResult.NetworkError -> message
        is SleepLogResult.ServerError -> message
    }
}
