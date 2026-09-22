package com.example.kenkosahealthapp_prog7314.ui.fasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.FastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
// State of the full fast list shown on the main fasting screen.
sealed class FastingUiState {
    object Loading : FastingUiState()
    data class Success(val fasts: List<FastDto>) : FastingUiState()
    data class Error(val message: String) : FastingUiState()
}

// State of the single fast loaded by the Add / Fast Details screen.
sealed class FastEditState {
    object Idle : FastEditState()
    object Loading : FastEditState()
    data class Loaded(val fast: FastDto) : FastEditState()
    data class Error(val message: String) : FastEditState()
}

// Shared ViewModel backing both FastingFragment (the list/overview screen)
// and AddFastFragment (create/view/edit a single fast). Talks to the
// FastRepository for all network access.
class FastingViewModel(
    private val repository: FastRepository = FastRepository()
) : ViewModel() {

    // Backing MutableStateFlow / read-only exposed StateFlow pair, so only this
    // ViewModel can push new state but the UI can only observe it.
    private val _uiState = MutableStateFlow<FastingUiState>(FastingUiState.Loading)
    val uiState: StateFlow<FastingUiState> = _uiState

    private val _editState = MutableStateFlow<FastEditState>(FastEditState.Idle)
    val editState: StateFlow<FastEditState> = _editState

    // ------------------------------------------------------------------ list
    // Loads (or refreshes) the full list of fasts.
    fun loadFasts() {
        viewModelScope.launch {
            // Only show the full-screen loading state on the very first load;
            // subsequent refreshes keep showing the existing list while fetching.
            if (_uiState.value !is FastingUiState.Success) _uiState.value = FastingUiState.Loading
            _uiState.value = when (val result = repository.getFasts()) {
                is ApiResult.Success -> FastingUiState.Success(result.data)
                else -> FastingUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    // "Start Fasting": begins a fast right now with the default goal.
    fun startNow(onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = repository.createFast(now, now + DEFAULT_GOAL_MILLIS)
            onResult(result is ApiResult.Success, result.errorMessage())
            loadFasts()
        }
    }

    // "End Fasting": marks the fast as finished as of now.
    fun endFast(fast: FastDto, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            // The end has to be after the start, even if the fast was only just begun.
            val end = maxOf(System.currentTimeMillis(), fast.startTime + 60_000L)
            val result = repository.updateFast(fast.fastId, endTime = end, active = false)
            onResult(result is ApiResult.Success, result.errorMessage())
            loadFasts()
        }
    }

    // Deletes a fast by ID; onResult is optional since some callers (e.g. cascading
    // cleanup) may not need a completion callback.
    fun deleteFast(
        fastId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteFast(fastId)
            onResult?.invoke(result is ApiResult.Success, result.errorMessage())
            loadFasts()
        }
    }

    // ------------------------------------------------------------- add / edit
    // Loads a single fast's full details for the Add/Details screen (edit mode).
    fun loadFast(fastId: String) {
        viewModelScope.launch {
            _editState.value = FastEditState.Loading
            _editState.value = when (val result = repository.getFast(fastId)) {
                is ApiResult.Success -> FastEditState.Loaded(result.data)
                else -> FastEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    // Creates a brand-new fast with the given start/end times.
    fun createFast(
        startTime: Long, endTime: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createFast(startTime, endTime)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    // Updates an existing fast's start/end times (used when manually editing
    // either an active or a finished fast).
    fun updateFast(
        fastId: String, startTime: Long, endTime: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateFast(fastId, startTime = startTime, endTime = endTime)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    // Extracts a user-facing error message from any non-success ApiResult,
    // or null if the result was actually successful.
    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }

    companion object {
        // A quick-started fast aims for 16 hours (the common 16:8 pattern)
        const val DEFAULT_GOAL_HOURS = 16L
        const val DEFAULT_GOAL_MILLIS = DEFAULT_GOAL_HOURS * 60 * 60 * 1000L
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------