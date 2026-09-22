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

/**
 * What the form needs to start: the saved metrics (if any) and which units to default to.
 *
 * [unitSystem] is the raw Settings value ("Metric" / "Imperial" / "UK"), kept alongside the
 * derived [defaultHeightUnit] / [defaultWeightUnit] so that saving can preserve "UK" instead of
 * collapsing it into "Imperial" when nothing on the form actually changed.
 */
data class MetricsFormData(
    val existing: UserMetrics?,
    val defaultHeightUnit: HeightUnit,
    val defaultWeightUnit: WeightUnit,
    val unitSystem: String
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

    /**
     * Saves the metrics, then best-effort syncs [unitSystem] back to Settings so a unit change
     * made on this form (e.g. switching to ft/in) is reflected there too.
     */
    fun save(metrics: UserMetrics, unitSystem: String, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        val uid = userId
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
}