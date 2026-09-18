package com.example.kenkosahealthapp_prog7314.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.SleepRecordDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class SleepUiState {
    object Loading : SleepUiState()
    data class Success(val records: List<SleepRecordDto>) : SleepUiState()
    data class Error(val message: String) : SleepUiState()
}

class SleepViewModel(private val repository: SleepRepository = SleepRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Loading)
    val uiState: StateFlow<SleepUiState> = _uiState

    init { loadSleep() }

    fun loadSleep() {
        viewModelScope.launch {
            _uiState.value = SleepUiState.Loading

            val calendar = Calendar.getInstance()
            val to = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val from = calendar.timeInMillis

            when (val result = repository.getSleep(from, to)) {
                is ApiResult.Success -> _uiState.value = SleepUiState.Success(result.data)
                is ApiResult.NetworkError -> _uiState.value = SleepUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = SleepUiState.Error(result.message)
                is ApiResult.ValidationError -> _uiState.value = SleepUiState.Error(result.message)
            }
        }
    }

    fun createSleep(
        sleepStart: Long, sleepEnd: Long, qualityRating: Int, notes: String?,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = repository.createSleep(sleepStart, sleepEnd, qualityRating, notes)) {
                is ApiResult.Success -> {
                    loadSleep()
                    onResult(true, null)
                }
                is ApiResult.ValidationError -> onResult(false, result.message)
                is ApiResult.NetworkError -> onResult(false, result.message)
                is ApiResult.ServerError -> onResult(false, result.message)
            }
        }
    }
}