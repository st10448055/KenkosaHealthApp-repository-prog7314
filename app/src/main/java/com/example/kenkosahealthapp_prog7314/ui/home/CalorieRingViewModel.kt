package com.example.kenkosahealthapp_prog7314.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.model.CalorieCalculator
import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.example.kenkosahealthapp_prog7314.data.repository.ApiResult
import com.example.kenkosahealthapp_prog7314.data.repository.DiaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Represents the UI state states for the calorie ring card on the home screen
sealed class CalorieUiState {
    object Loading : CalorieUiState()
    data class Success(
        val consumed: Int,
        val target: Int,
        val remaining: Int,
        val carbsG: Int,
        val proteinG: Int,
        val fatG: Int,
        val statusLabel: String
    ) : CalorieUiState()
    data class Error(val message: String) : CalorieUiState()
}

// ViewModel responsible for fetching profile metrics and calculating macro breakdown
class CalorieRingViewModel(
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    // Firebase Auth instance reference
    private val auth = FirebaseAuth.getInstance()

    // Firestore database instance reference
    private val firestore = FirebaseFirestore.getInstance()

    // Internal state flow for calorie UI state
    private val _uiState = MutableStateFlow<CalorieUiState>(CalorieUiState.Loading)

    // Exposed read-only state flow for UI observations
    val uiState: StateFlow<CalorieUiState> = _uiState


    // Fetches user profile metrics and logged meals to update state
    fun loadCalories() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = CalorieUiState.Error("Not signed in")
            return
        }

        // Emit loading state before fetching data
        _uiState.value = CalorieUiState.Loading

        // Retrieve user metrics from Firestore
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val metrics = UserMetrics.fromSnapshot(doc)
                if (metrics == null) {
                    _uiState.value = CalorieUiState.Error("Add your body metrics to see your calorie target")
                    return@addOnSuccessListener
                }

                // Compute daily target calories based on user metrics
                val target = CalorieCalculator.dailyCalorieTarget(metrics)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Fetch meal entries logged for today from repository
                viewModelScope.launch {
                    when (val result = diaryRepository.getMeals(today)) {
                        is ApiResult.Success -> {
                            val meals = result.data
                            val consumed = meals.sumOf { it.calories }.roundToInt()
                            val carbs = meals.sumOf { it.carbsG }.roundToInt()
                            val protein = meals.sumOf { it.proteinG }.roundToInt()
                            val fat = meals.sumOf { it.fatG }.roundToInt()
                            val remaining = (target - consumed).coerceAtLeast(0)
                            val status = if (consumed <= target) "On Track" else "Over Target"

                            // Emit success state with calculated macros and status
                            _uiState.value = CalorieUiState.Success(
                                consumed = consumed,
                                target = target,
                                remaining = remaining,
                                carbsG = carbs,
                                proteinG = protein,
                                fatG = fat,
                                statusLabel = status
                            )
                        }
                        is ApiResult.ValidationError -> _uiState.value = CalorieUiState.Error(result.message)
                        is ApiResult.NetworkError -> _uiState.value = CalorieUiState.Error(result.message)
                        is ApiResult.ServerError -> _uiState.value = CalorieUiState.Error(result.message)
                    }
                }
            }
            .addOnFailureListener {
                _uiState.value = CalorieUiState.Error("Couldn't load your profile")
            }
    }
}
//---------------------------------------END OF FILE-------------------------------------------------