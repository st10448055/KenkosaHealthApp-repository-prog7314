package com.example.kenkosahealthapp_prog7314.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MedicationUiState {
    object Loading : MedicationUiState()
    data class Success(val medications: List<MedicationDto>) : MedicationUiState()
    data class Error(val message: String) : MedicationUiState()
}

/** State of the single medication loaded by the Add / Edit screen. */
sealed class MedicationEditState {
    object Idle : MedicationEditState()
    object Loading : MedicationEditState()
    data class Loaded(val medication: MedicationDto) : MedicationEditState()
    data class Error(val message: String) : MedicationEditState()
}

class MedicationViewModel(
    private val repository: MedicationRepository = MedicationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MedicationUiState>(MedicationUiState.Loading)
    val uiState: StateFlow<MedicationUiState> = _uiState

    private val _editState = MutableStateFlow<MedicationEditState>(MedicationEditState.Idle)
    val editState: StateFlow<MedicationEditState> = _editState

    // ------------------------------------------------------------------ list

    fun loadMedications() {
        viewModelScope.launch {
            // Only show the spinner on the very first load so returning to the screen doesn't flicker.
            if (_uiState.value !is MedicationUiState.Success) {
                _uiState.value = MedicationUiState.Loading
            }
            _uiState.value = when (val result = repository.getMedications()) {
                is ApiResult.Success -> MedicationUiState.Success(result.data)
                else -> MedicationUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    /** Ticks the dose on/off. Updates the UI straight away and rolls back (by reloading) if the call fails. */
    fun setTaken(medication: MedicationDto, date: String, taken: Boolean) {
        val current = (_uiState.value as? MedicationUiState.Success)?.medications ?: return
        val newDates = if (taken) (medication.takenDates + date).distinct() else medication.takenDates - date
        _uiState.value = MedicationUiState.Success(
            current.map {
                if (it.medicationId == medication.medicationId) it.copy(takenDates = newDates) else it
            }
        )

        viewModelScope.launch {
            if (repository.setTaken(medication.medicationId, date, taken) !is ApiResult.Success) {
                loadMedications()
            }
        }
    }

    fun deleteMedication(medicationId: String) {
        viewModelScope.launch {
            repository.deleteMedication(medicationId)
            loadMedications()
        }
    }

    // ------------------------------------------------------------- add / edit

    fun loadMedication(medicationId: String) {
        viewModelScope.launch {
            _editState.value = MedicationEditState.Loading
            _editState.value = when (val result = repository.getMedication(medicationId)) {
                is ApiResult.Success -> MedicationEditState.Loaded(result.data)
                else -> MedicationEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun createMedication(
        category: String, name: String, dosage: String, frequency: String,
        remindersEnabled: Boolean, reminderTime: String, startDate: String,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createMedication(
                category, name, dosage, frequency, remindersEnabled, reminderTime, startDate
            )
            onResult(result is ApiResult.Success, result.errorMessage())
        }
    }

    fun updateMedication(
        medicationId: String,
        category: String, name: String, dosage: String, frequency: String,
        remindersEnabled: Boolean, reminderTime: String, startDate: String?,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateMedication(
                medicationId, category, name, dosage, frequency, remindersEnabled, reminderTime, startDate
            )
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
