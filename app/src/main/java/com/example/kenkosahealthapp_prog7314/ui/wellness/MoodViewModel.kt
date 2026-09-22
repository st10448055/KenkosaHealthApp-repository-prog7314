package com.example.kenkosahealthapp_prog7314.ui.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.MoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Represents the different states of the Recent Moods list.
sealed class MoodUiState {
    object Loading : MoodUiState()
    data class Success(val entries: List<MoodEntryDto>) : MoodUiState()
    data class Error(val message: String) : MoodUiState()
}

// Represents the state of a single mood entry when it is being loaded for editing.
sealed class MoodEditState {
    object Idle : MoodEditState()
    object Loading : MoodEditState()
    data class Loaded(val entry: MoodEntryDto) : MoodEditState()
    data class Error(val message: String) : MoodEditState()
}

class MoodViewModel(
    // Repository responsible for communicating with the mood data source/API.
    private val repository: MoodRepository = MoodRepository()
) : ViewModel() {

    // Mutable state is kept private so only the ViewModel can update it.
    private val _uiState =
        MutableStateFlow<MoodUiState>(MoodUiState.Loading)

    // The UI observes this read-only StateFlow for changes to the mood list.
    val uiState: StateFlow<MoodUiState> = _uiState

    // Separate state is used when loading one mood for the edit screen.
    private val _editState =
        MutableStateFlow<MoodEditState>(MoodEditState.Idle)

    // The edit screen observes this without being able to change the state directly.
    val editState: StateFlow<MoodEditState> = _editState

    // ------------------------------------------------------------------ list

    // Load all mood entries for the Recent Moods list.
    fun loadMoods() {
        // viewModelScope keeps the coroutine tied to the ViewModel's lifecycle.
        viewModelScope.launch {

            // Only show the loading state if we do not already have successful data.
            // This prevents the existing list from disappearing during a refresh.
            if (_uiState.value !is MoodUiState.Success) {
                _uiState.value = MoodUiState.Loading
            }

            // Ask the repository for the latest mood entries and convert the result
            // into a state that the Fragment can display.
            _uiState.value = when (val result = repository.getMoods()) {
                is ApiResult.Success ->
                    MoodUiState.Success(result.data)

                else ->
                    MoodUiState.Error(
                        result.errorMessage() ?: "Something went wrong"
                    )
            }
        }
    }

    // Delete a mood entry using its unique ID.
    fun deleteMood(
        moodId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteMood(moodId)

            // Tell the Fragment whether the deletion succeeded and provide
            // an error message if the repository returned an error.
            onResult?.invoke(
                result is ApiResult.Success,
                result.errorMessage()
            )

            // Refresh the list so the deleted mood is removed from the UI.
            loadMoods()
        }
    }

    // ------------------------------------------------------------- log / edit

    // Load one specific mood entry for the Log Mood screen in edit mode.
    fun loadMood(moodId: String) {
        viewModelScope.launch {
            _editState.value = MoodEditState.Loading

            // Request the selected mood and convert the result into an edit state.
            _editState.value = when (val result = repository.getMood(moodId)) {
                is ApiResult.Success ->
                    MoodEditState.Loaded(result.data)

                else ->
                    MoodEditState.Error(
                        result.errorMessage() ?: "Something went wrong"
                    )
            }
        }
    }

    // Create a new mood entry.
    fun createMood(
        mood: String,
        feelings: List<String>,
        loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createMood(
                mood,
                feelings,
                loggedAt
            )

            // Return the operation result to the Fragment so it can
            // navigate away or display an error message.
            onResult(
                result is ApiResult.Success,
                result.errorMessage()
            )
        }
    }

    // Update an existing mood entry.
    fun updateMood(
        moodId: String,
        mood: String,
        feelings: List<String>,
        loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateMood(
                moodId,
                mood,
                feelings,
                loggedAt
            )

            // Let the Fragment know whether the update was successful.
            onResult(
                result is ApiResult.Success,
                result.errorMessage()
            )
        }
    }

    // Extract a user-friendly error message from any failed API result.
    // Successful requests do not have an error, so they return null.
    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }
}