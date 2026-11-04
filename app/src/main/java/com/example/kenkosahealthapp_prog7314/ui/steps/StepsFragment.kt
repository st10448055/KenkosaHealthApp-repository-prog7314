package com.example.kenkosahealthapp_prog7314.ui.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository
import com.example.kenkosahealthapp_prog7314.databinding.FragmentStepsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Today's steps from the phone's hardware step counter.
 *
 * The sensor reports steps since boot, so a per-day baseline is kept in SharedPreferences ("steps").
 * "today_steps" is read by the Goals screen to fill STEPS goals automatically.
 */
class StepsFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentStepsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private val prefs by lazy { requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private var goal = 10_000
    private var steps = 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "ACTIVITY_RECOGNITION granted=$granted")
            if (granted) startCounting() else _binding?.textStepsInfo?.setText(R.string.steps_permission_needed)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.rootSteps.updatePadding(top = bars.top + 20.dp(), bottom = bars.bottom)
            insets
        }

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Daily step goal from Settings
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            goal = runCatching { UserRepository().getSettings(uid)?.dailyStepGoal }.getOrNull() ?: goal
            render()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
            == PackageManager.PERMISSION_GRANTED
        ) startCounting() else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun startCounting() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            Log.w(TAG, "No step counter sensor on this device")
            _binding?.textStepsInfo?.setText(R.string.steps_no_sensor)
            return
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        Log.d(TAG, "Step listener registered")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values[0]
        val today = LocalDate.now()

        if (prefs.getString(KEY_DAY, null) != today.toString()) {
            // New day: keep steps taken since the last reading if that was yesterday and the phone didn't reboot.
            val lastTotal = prefs.getFloat(KEY_LAST_TOTAL, -1f)
            val wasYesterday = prefs.getString(KEY_DAY, null) == today.minusDays(1).toString()
            val baseline = if (wasYesterday && lastTotal in 0f..total) lastTotal else total
            prefs.edit().putString(KEY_DAY, today.toString()).putFloat(KEY_BASELINE, baseline).apply()
            Log.d(TAG, "New day baseline=$baseline")
        }

        steps = (total - prefs.getFloat(KEY_BASELINE, total)).toInt().coerceAtLeast(0)
        prefs.edit().putFloat(KEY_LAST_TOTAL, total).putInt(KEY_TODAY_STEPS, steps).apply()
        Log.d(TAG, "Steps today: $steps")
        render()
    }

    private fun render() {
        val b = _binding ?: return
        b.textSteps.text = String.format(Locale.getDefault(), "%,d", steps)
        b.textStepsGoal.text = getString(R.string.steps_goal, String.format(Locale.getDefault(), "%,d", goal))
        b.progressSteps.setProgressCompat((steps * 100 / goal.coerceAtLeast(1)).coerceIn(0, 100), true)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroyView() {
        sensorManager.unregisterListener(this)
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "StepsFragment"
        const val PREFS = "steps"
        const val KEY_DAY = "day"
        const val KEY_BASELINE = "baseline"
        const val KEY_LAST_TOTAL = "last_total"
        const val KEY_TODAY_STEPS = "today_steps"
    }
}