package com.example.kenkosahealthapp_prog7314.ui.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.MoodRepository
import com.example.kenkosahealthapp_prog7314.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
    // Offline-first repository backed by Room.
    private val repository: MoodRepository = MoodRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoodUiState>(MoodUiState.Loading)
    val uiState: StateFlow<MoodUiState> = _uiState

    private val _editState = MutableStateFlow<MoodEditState>(MoodEditState.Idle)
    val editState: StateFlow<MoodEditState> = _editState

    // True when the device has an internet connection.
    val isOnline: StateFlow<Boolean> = NetworkMonitor.isOnline

    // Number of changes made offline that are still waiting to upload.
    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // The list always comes from the local database, so it works offline
        // and updates automatically whenever the data changes.
        viewModelScope.launch {
            repository.observeMoods().collect { entries ->
                _uiState.value = MoodUiState.Success(entries)
            }
        }
    }

    // ------------------------------------------------------------------ list

    // Syncs with the server in the background; the list updates itself afterwards.
    fun loadMoods() {
        viewModelScope.launch { repository.refresh() }
    }

    // Delete a mood entry (works offline; the deletion syncs later).
    fun deleteMood(
        moodId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteMood(moodId)
            onResult?.invoke(result is ApiResult.Success, result.errorMessage())
        }
    }

    // ------------------------------------------------------------- log / edit

    // Load one specific mood entry for the Log Mood screen in edit mode.
    fun loadMood(moodId: String) {
        viewModelScope.launch {
            _editState.value = MoodEditState.Loading
            _editState.value = when (val result = repository.getMood(moodId)) {
                is ApiResult.Success -> MoodEditState.Loaded(result.data)
                else -> MoodEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    // Create a new mood entry (works offline).
    fun createMood(
        mood: String,
        feelings: List<String>,
        loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createMood(mood, feelings, loggedAt)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    // Update an existing mood entry (works offline).
    fun updateMood(
        moodId: String,
        mood: String,
        feelings: List<String>,
        loggedAt: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateMood(moodId, mood, feelings, loggedAt)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    // Extract a user-friendly error message from any failed result.
    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }
}
// References:
// Android Developers (n.d.) Read network state. Available at:
//   https://developer.android.com/training/basics/network-ops/reading-network-state (Accessed: 5 November 2026).