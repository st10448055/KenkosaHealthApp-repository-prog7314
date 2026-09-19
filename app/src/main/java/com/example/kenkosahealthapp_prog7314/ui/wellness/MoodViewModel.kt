package com.example.kenkosahealthapp_prog7314.ui.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.MoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MoodUiState {
    object Loading : MoodUiState()
    data class Success(val entries: List<MoodEntryDto>) : MoodUiState()
    data class Error(val message: String) : MoodUiState()
}

/** State of the single mood entry loaded by the Log Mood screen (edit mode). */
sealed class MoodEditState {
    object Idle : MoodEditState()
    object Loading : MoodEditState()
    data class Loaded(val entry: MoodEntryDto) : MoodEditState()
    data class Error(val message: String) : MoodEditState()
}

class MoodViewModel(
    private val repository: MoodRepository = MoodRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoodUiState>(MoodUiState.Loading)
    val uiState: StateFlow<MoodUiState> = _uiState

    private val _editState = MutableStateFlow<MoodEditState>(MoodEditState.Idle)
    val editState: StateFlow<MoodEditState> = _editState

    // ------------------------------------------------------------------ list

    fun loadMoods() {
        viewModelScope.launch {
            if (_uiState.value !is MoodUiState.Success) _uiState.value = MoodUiState.Loading
            _uiState.value = when (val result = repository.getMoods()) {
                is ApiResult.Success -> MoodUiState.Success(result.data)
                else -> MoodUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun deleteMood(
        moodId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteMood(moodId)
            onResult?.invoke(result is ApiResult.Success, result.errorMessage())
            loadMoods()
        }
    }

    // ------------------------------------------------------------- log / edit

    fun loadMood(moodId: String) {
        viewModelScope.launch {
            _editState.value = MoodEditState.Loading
            _editState.value = when (val result = repository.getMood(moodId)) {
                is ApiResult.Success -> MoodEditState.Loaded(result.data)
                else -> MoodEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun createMood(
        mood: String, feelings: List<String>, loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createMood(mood, feelings, loggedAt)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    fun updateMood(
        moodId: String, mood: String, feelings: List<String>, loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateMood(moodId, mood, feelings, loggedAt)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }
}
