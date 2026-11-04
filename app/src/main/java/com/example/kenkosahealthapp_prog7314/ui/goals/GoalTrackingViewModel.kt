package com.example.kenkosahealthapp_prog7314.ui.goals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.network.SaveTrackedGoalRequest
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.data.repository.GoalTrackingRepository
import com.example.kenkosahealthapp_prog7314.data.repository.TrackingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class GoalTrackingUiState {
    object Loading : GoalTrackingUiState()
    data class Success(val goals: List<TrackedGoalDto>) : GoalTrackingUiState()
    data class Error(val message: String) : GoalTrackingUiState()
}

/** State of the single goal loaded by the Add / Edit Goal screen. */
sealed class GoalEditState {
    object Idle : GoalEditState()
    object Loading : GoalEditState()
    data class Loaded(val goal: TrackedGoalDto) : GoalEditState()
    data class Error(val message: String) : GoalEditState()
}

class GoalTrackingViewModel(
    private val repository: GoalTrackingRepository = GoalTrackingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoalTrackingUiState>(GoalTrackingUiState.Loading)
    val uiState: StateFlow<GoalTrackingUiState> = _uiState

    private val _editState = MutableStateFlow<GoalEditState>(GoalEditState.Idle)
    val editState: StateFlow<GoalEditState> = _editState

    // ------------------------------------------------------------------ list

    /**
     * Loads the goals. WATER and SLEEP goals are first filled by the server from what the user logged;
     * STEPS goals are filled from [todaySteps] (the phone's step counter), when it has a reading for today.
     */
    fun loadGoals(todaySteps: Int? = null) {
        viewModelScope.launch {
            // Only show the spinner on the very first load so returning to the screen doesn't flicker.
            if (_uiState.value !is GoalTrackingUiState.Success) _uiState.value = GoalTrackingUiState.Loading

            // Failure here is fine: the screen still loads with whatever progress is already stored.
            val synced = repository.syncGoals()
            if (synced !is TrackingResult.Success) Log.w(TAG, "Goal sync skipped: ${synced.errorMessage()}")

            if (todaySteps != null) {
                val today = LocalDate.now()
                val goals = (synced as? TrackingResult.Success)?.data
                    ?: (repository.getGoals() as? TrackingResult.Success)?.data.orEmpty()
                goals.filter { it.category == "STEPS" && GoalProgress.valueOn(it, today) != todaySteps.toDouble() }
                    .forEach {
                        Log.d(TAG, "Logging $todaySteps steps to goal ${it.goalId}")
                        repository.logProgress(it.goalId, today.toString(), todaySteps.toDouble())
                    }
            }

            _uiState.value = when (val result = repository.getGoals()) {
                is TrackingResult.Success -> GoalTrackingUiState.Success(result.data)
                else -> GoalTrackingUiState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    /** Sets the total for [date] (not an increment), then refreshes so the bars and the streak update. */
    fun logProgress(
        goalId: String, date: String, value: Double,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.logProgress(goalId, date, value)
            onResult(result is TrackingResult.Success, result.errorMessage())
            loadGoals()
        }
    }

    fun deleteGoal(
        goalId: String,
        onResult: ((success: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteGoal(goalId)
            onResult?.invoke(result is TrackingResult.Success, result.errorMessage())
            loadGoals()
        }
    }

    // ------------------------------------------------------------- add / edit

    fun loadGoal(goalId: String) {
        viewModelScope.launch {
            _editState.value = GoalEditState.Loading
            _editState.value = when (val result = repository.getGoal(goalId)) {
                is TrackingResult.Success -> GoalEditState.Loaded(result.data)
                else -> GoalEditState.Error(result.errorMessage() ?: "Something went wrong")
            }
        }
    }

    fun createGoal(
        request: SaveTrackedGoalRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createGoal(request)
            onResult(result is TrackingResult.Success, result.errorMessage())
        }
    }

    fun updateGoal(
        goalId: String, request: SaveTrackedGoalRequest,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateGoal(goalId, request)
            onResult(result is TrackingResult.Success, result.errorMessage())
        }
    }

    private fun TrackingResult<*>.errorMessage(): String? = when (this) {
        is TrackingResult.Success -> null
        is TrackingResult.ValidationError -> message
        is TrackingResult.NetworkError -> message
        is TrackingResult.ServerError -> message
    }

    private companion object {
        const val TAG = "GoalTrackingVM"
    }
}