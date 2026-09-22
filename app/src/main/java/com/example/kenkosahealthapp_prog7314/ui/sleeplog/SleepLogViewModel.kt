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

// Represents the different states that the sleep log list can be in.
sealed class SleepLogUiState {
    // The sleep records are currently being loaded.
    object Loading : SleepLogUiState()

    // The records were successfully loaded and are stored in this state.
    data class Success(val records: List<SleepLogDto>) : SleepLogUiState()

    // Something went wrong while loading the records.
    data class Error(val message: String) : SleepLogUiState()
}

// Represents the states for loading one individual sleep record
// on the Add/Edit Sleep screen.
sealed class SleepEditState {
    // No record is currently being loaded or edited.
    object Idle : SleepEditState()

    // The individual record is currently being loaded.
    object Loading : SleepEditState()

    // The record was successfully loaded.
    data class Loaded(val record: SleepLogDto) : SleepEditState()

    // An error occurred while loading the individual record.
    data class Error(val message: String) : SleepEditState()
}

class SleepLogViewModel(
    // The ViewModel uses the repository to communicate with the sleep log data.
    // A default repository is provided so the ViewModel can create one itself.
    private val repository: SleepLogRepository = SleepLogRepository()
) : ViewModel() {

    // MutableStateFlow is kept private so that only the ViewModel can change the state.
    private val _uiState =
        MutableStateFlow<SleepLogUiState>(SleepLogUiState.Loading)

    // The screen receives a read-only StateFlow so it can observe changes
    // without being able to modify the state directly.
    val uiState: StateFlow<SleepLogUiState> = _uiState

    // Holds the state of a single sleep record when adding or editing it.
    private val _editState =
        MutableStateFlow<SleepEditState>(SleepEditState.Idle)

    // Exposes the edit state to the UI as a read-only StateFlow.
    val editState: StateFlow<SleepEditState> = _editState

    // -------------------------------------------------- list

    // Loads all of the user's sleep records from the repository.
    fun loadSleep() {
        // viewModelScope automatically cancels the coroutine if the ViewModel is destroyed.
        viewModelScope.launch {

            // Only show the loading state if the list has not already loaded.
            // This prevents the screen from flashing a spinner every time it reloads.
            if (_uiState.value !is SleepLogUiState.Success) {
                _uiState.value = SleepLogUiState.Loading
            }

            // Calls the repository to retrieve the sleep records.
            // The result is converted into a UI state that the screen understands.
            _uiState.value = when (val result = repository.getSleep()) {

                // If the request was successful, store the returned records.
                is SleepLogResult.Success ->
                    SleepLogUiState.Success(result.data)

                // If the request failed, display the error message.
                else ->
                    SleepLogUiState.Error(
                        result.errorMessage() ?: "Something went wrong"
                    )
            }
        }
    }

    // Deletes a sleep record using its unique ID.
    fun deleteSleep(
        sleepId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {

            // Sends the sleep ID to the repository so it can perform the delete operation.
            val result = repository.deleteSleep(sleepId)

            // Tells the UI whether the delete was successful and provides
            // an error message if something went wrong.
            onResult?.invoke(
                result is SleepLogResult.Success,
                result.errorMessage()
            )

            // Reloads the list after deleting so the UI shows the updated records.
            loadSleep()
        }
    }

    // -------------------------------------------------- add / edit

    // Loads one specific sleep record using its ID.
    // This is mainly used when opening an existing record for editing.
    fun loadRecord(sleepId: String) {
        viewModelScope.launch {

            // Tell the UI that the individual record is currently loading.
            _editState.value = SleepEditState.Loading

            // Request the record from the repository and convert the result
            // into the appropriate editing state.
            _editState.value = when (val result =
                repository.getSleepRecord(sleepId)
            ) {

                // The record was successfully retrieved.
                is SleepLogResult.Success ->
                    SleepEditState.Loaded(result.data)

                // The record could not be retrieved.
                else ->
                    SleepEditState.Error(
                        result.errorMessage() ?: "Something went wrong"
                    )
            }
        }
    }

    // Creates a new sleep record using the information in the request.
    fun createSleep(
        request: SaveSleepLogRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {

            // Sends the new sleep record to the repository to be saved.
            val result = repository.createSleep(request)

            // Notifies the UI whether the save was successful.
            onResult(
                result is SleepLogResult.Success,
                result.errorMessage()
            )
        }
    }

    // Updates an existing sleep record using its ID and the new information.
    fun updateSleep(
        sleepId: String,
        request: SaveSleepLogRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {

            // Sends the ID and updated data to the repository.
            val result = repository.updateSleep(sleepId, request)

            // Notifies the UI whether the update was successful.
            onResult(
                result is SleepLogResult.Success,
                result.errorMessage()
            )
        }
    }

    // Converts the different repository errors into a simple message
    // that can be displayed by the UI.
    private fun SleepLogResult<*>.errorMessage(): String? = when (this) {

        // A successful result does not have an error message.
        is SleepLogResult.Success -> null

        // Returns the error message when there was a network problem.
        is SleepLogResult.NetworkError -> message

        // Returns the error message when the server returned an error.
        is SleepLogResult.ServerError -> message
    }
}
