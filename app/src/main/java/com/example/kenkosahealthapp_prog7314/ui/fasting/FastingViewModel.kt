package com.example.kenkosahealthapp_prog7314.ui.fasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.FastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class FastingUiState {
    object Loading : FastingUiState()
    data class Success(val fasts: List<FastDto>) : FastingUiState()
    data class Error(val message: String) : FastingUiState()
}

/** State of the single fast loaded by the Add / Fast Details screen. */
sealed class FastEditState {
    object Idle : FastEditState()
    object Loading : FastEditState()
    data class Loaded(val fast: FastDto) : FastEditState()
    data class Error(val message: String) : FastEditState()
}

class FastingViewModel(
    private val repository: FastRepository = FastRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FastingUiState>(FastingUiState.Loading)
    val uiState: StateFlow<FastingUiState> = _uiState

    private val _editState = MutableStateFlow<FastEditState>(FastEditState.Idle)
    val editState: StateFlow<FastEditState> = _editState

    // ------------------------------------------------------------------ list

    fun loadFasts() {
        viewModelScope.launch {
            if (_uiState.value !is FastingUiState.Success) _uiState.value = FastingUiState.Loading
            _uiState.value = when (val result = repository.getFasts()) {
                is ApiResult.Success -> FastingUiState.Success(result.data)
                else -> FastingUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    /** "Start Fasting": begins a fast right now with the default goal. */
    fun startNow(onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = repository.createFast(now, now + DEFAULT_GOAL_MILLIS)
            onResult(result is ApiResult.Success, result.errorMessage())
            loadFasts()
        }
    }

    /** "End Fasting": marks the fast as finished as of now. */
    fun endFast(fast: FastDto, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            // The end has to be after the start, even if the fast was only just begun.
            val end = maxOf(System.currentTimeMillis(), fast.startTime + 60_000L)
            val result = repository.updateFast(fast.fastId, endTime = end, active = false)
            onResult(result is ApiResult.Success, result.errorMessage())
            loadFasts()
        }
    }

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

    fun loadFast(fastId: String) {
        viewModelScope.launch {
            _editState.value = FastEditState.Loading
            _editState.value = when (val result = repository.getFast(fastId)) {
                is ApiResult.Success -> FastEditState.Loaded(result.data)
                else -> FastEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun createFast(
        startTime: Long, endTime: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createFast(startTime, endTime)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    fun updateFast(
        fastId: String, startTime: Long, endTime: Long,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateFast(fastId, startTime = startTime, endTime = endTime)
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }

    companion object {
        /** A quick-started fast aims for 16 hours (the common 16:8 pattern). Change the length here. */
        const val DEFAULT_GOAL_HOURS = 16L
        const val DEFAULT_GOAL_MILLIS = DEFAULT_GOAL_HOURS * 60 * 60 * 1000L
    }
}