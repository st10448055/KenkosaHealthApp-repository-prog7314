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

/** What the form needs to start: the saved metrics (if any) and which units to default to. */
data class MetricsFormData(
    val existing: UserMetrics?,
    val defaultHeightUnit: HeightUnit,
    val defaultWeightUnit: WeightUnit
)

class MetricsViewModel(
    private val metricsRepository: MetricsRepository = MetricsRepository()
) : ViewModel() {

    /** null while loading. */
    private val _formData = MutableStateFlow<MetricsFormData?>(null)
    val formData: StateFlow<MetricsFormData?> = _formData

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        load()
    }

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

            // New users start in the unit system they picked in Settings (Metric / Imperial / UK).
            val imperial = unitSystem != "Metric"
            _formData.value = MetricsFormData(
                existing = existing,
                defaultHeightUnit = if (imperial) HeightUnit.FT_IN else HeightUnit.CM,
                defaultWeightUnit = if (imperial) WeightUnit.LB else WeightUnit.KG
            )
        }
    }

    fun save(metrics: UserMetrics, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        val uid = userId
        if (uid == null) {
            onResult(false, null)
            return
        }
        viewModelScope.launch {
            try {
                metricsRepository.saveMetrics(uid, metrics)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}
