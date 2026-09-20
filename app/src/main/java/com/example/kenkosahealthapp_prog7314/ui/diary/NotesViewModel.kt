package com.example.kenkosahealthapp_prog7314.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val noteText: String) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}

class NotesViewModel(
    private val notesRepository: NotesRepository = NotesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState

    init {
        loadNote()
    }

    fun loadNote() {
        viewModelScope.launch {
            _uiState.value = NotesUiState.Loading
            when (val result = notesRepository.getTodaysNote()) {
                is ApiResult.Success -> _uiState.value = NotesUiState.Success(result.data.noteText)
                is ApiResult.ValidationError -> _uiState.value = NotesUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = NotesUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = NotesUiState.Error(result.message)
            }
        }
    }

    fun saveNote(noteText: String) {
        viewModelScope.launch {
            when (val result = notesRepository.updateNote(noteText)) {
                is ApiResult.Success -> _uiState.value = NotesUiState.Success(result.data.noteText)
                is ApiResult.ValidationError -> _uiState.value = NotesUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = NotesUiState.Error(result.message)
                is ApiResult.ServerError -> _uiState.value = NotesUiState.Error(result.message)
            }
        }
    }
}