package com.example.kenkosahealthapp_prog7314.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.model.HeightUnit
import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.example.kenkosahealthapp_prog7314.data.model.WeightUnit
import com.example.kenkosahealthapp_prog7314.data.repository.MetricsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Contains the saved metrics and the unit settings used to display the form.
data class MetricsFormData(
    val existing: UserMetrics?,
    val defaultHeightUnit: HeightUnit,
    val defaultWeightUnit: WeightUnit,
    val unitSystem: String
)

class MetricsViewModel(
    private val metricsRepository: MetricsRepository = MetricsRepository()
) : ViewModel() {

    // Holds the form data once it has been loaded.
    private val _formData = MutableStateFlow<MetricsFormData?>(null)
    val formData: StateFlow<MetricsFormData?> = _formData

    // Gets the currently signed-in Firebase user's ID.
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        load()
    }

    // Loads the user's saved metrics and unit preference.
    private fun load() {
        viewModelScope.launch {
            val uid = userId
            var existing: UserMetrics? = null
            var unitSystem = "Metric"

            if (uid != null) {
                // If either read fails (offline, say) the form still opens, just empty with metric defaults.
                existing = runCatching { metricsRepository.getMetrics(uid) }.getOrNull()
                unitSystem = runCatching { metricsRepository.getUnitSystem(uid) }.getOrNull() ?: "Metric"
            }

            // Units shown always follow Settings (Metric / Imperial / UK), not whatever was
            // saved with a previous metrics entry - that keeps the two screens in sync.
            val imperial = unitSystem != "Metric"
            _formData.value = MetricsFormData(
                existing = existing,
                defaultHeightUnit = if (imperial) HeightUnit.FT_IN else HeightUnit.CM,
                defaultWeightUnit = if (imperial) WeightUnit.LB else WeightUnit.KG,
                unitSystem = unitSystem
            )
        }
    }

    // Saves the metrics and updates the user's unit preference in Settings.
    fun save(metrics: UserMetrics, unitSystem: String, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        val uid = userId
        // Cannot save metrics without a signed-in user.
        if (uid == null) {
            onResult(false, null)
            return
        }
        viewModelScope.launch {
            try {
                metricsRepository.saveMetrics(uid, metrics)
                // A Settings sync hiccup shouldn't block the metrics save itself.
                runCatching { metricsRepository.setUnitSystem(uid, unitSystem) }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}//---------------------------------------END OF FILE-------------------------------------------------