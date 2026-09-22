package com.example.kenkosahealthapp_prog7314.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Represents the possible states of the medication list screen.
sealed class MedicationUiState {
    // The medication list is currently loading.
    object Loading : MedicationUiState()

    // The medication list was successfully retrieved.
    data class Success(val medications: List<MedicationDto>) : MedicationUiState()

    // An error occurred while loading the medication list.
    data class Error(val message: String) : MedicationUiState()
}

// Represents the possible states of the medication list screen.
sealed class MedicationEditState {
    object Idle : MedicationEditState()

    // The medication list is currently loading.
    object Loading : MedicationEditState()
    data class Loaded(val medication: MedicationDto) : MedicationEditState()
    data class Error(val message: String) : MedicationEditState()
}

// ViewModel responsible for managing medication-related screen state
// and communicating with the MedicationRepository.
class MedicationViewModel(
    private val repository: MedicationRepository = MedicationRepository()
) : ViewModel() {

    // Public read-only version of the medication list state.
    private val _uiState = MutableStateFlow<MedicationUiState>(MedicationUiState.Loading)
    val uiState: StateFlow<MedicationUiState> = _uiState

    // Internal state for the Add/Edit Medication screen.
    private val _editState = MutableStateFlow<MedicationEditState>(MedicationEditState.Idle)
    val editState: StateFlow<MedicationEditState> = _editState

    // ------------------------------------------------------------------ list

    // Retrieves all medications from the repository.
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

    // Marks a medication as taken or not taken for a specific date.
    fun setTaken(medication: MedicationDto, date: String, taken: Boolean) {
        // Get the currently displayed medication list.
        // If there is no successful list yet, there is nothing to update.
        val current = (_uiState.value as? MedicationUiState.Success)?.medications ?: return
        val newDates = if (taken) (medication.takenDates + date).distinct() else medication.takenDates - date

        // Immediately update the local UI.
        // This gives the user instant feedback without waiting for the API.
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

    fun deleteMedication(
        medicationId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteMedication(medicationId)
            onResult?.invoke(result is ApiResult.Success, result.errorMessage())
            loadMedications()
        }
    }

    // ------------------------------------------------------------- add / edit
    // Loads one medication from the server for the edit screen.
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

    // Updates an existing medication.
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

    // Converts the different ApiResult error types into one String message.
    // This keeps error handling in the ViewModel simple for the Fragment.
    private fun ApiResult<*>.errorMessage(): String? = when (this) {
        is ApiResult.Success -> null
        is ApiResult.ValidationError -> message
        is ApiResult.NetworkError -> message
        is ApiResult.ServerError -> message
    }
}
//---------------------------------------END OF FILE-------------------------------------------------