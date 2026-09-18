package com.example.kenkosahealthapp_prog7314.ui.sleep

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.SleepRecordDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSleepBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class SleepFragment : Fragment() {

    private var _binding: FragmentSleepBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SleepViewModel by viewModels()
    private val goalApi = RetrofitInstance.goalApi

    // Fallback used only if the user has no SLEEP-category goal yet,
    // or the goals request fails (e.g. offline). Overwritten by
    // loadSleepGoalTarget() whenever a real goal is found.
    private var targetSleepMinutes = 8 * 60
    private var latestRecords: List<SleepRecordDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonAddSleep.setOnClickListener {
            findNavController().navigate(R.id.action_sleep_to_addSleep)
        }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the goal target first, then load sleep data
        // this way renderSummary() always uses the latest known target, in case the
        // user just edited their Sleep goal on the Goals screen.
        loadSleepGoalTarget()
    }

    private fun loadSleepGoalTarget() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val goals = goalApi.getGoals()
                val sleepGoal = goals.firstOrNull { it.category == "SLEEP" }
                if (sleepGoal != null && sleepGoal.targetValue > 0) {
                    targetSleepMinutes = (sleepGoal.targetValue * 60).toInt()
                }
                // If no SLEEP goal exists, keep the existing fallback/previous value.
            } catch (e: Exception) {
                // Network failure, no auth, etc. which keeps whatever target we
                // already have (fallback or last successful fetch) so the
                // Sleep screen still renders correctly.
            } finally {
                viewModel.loadSleep()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SleepUiState.Loading -> {
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.textError.visibility = View.GONE
                        }
                        is SleepUiState.Success -> {
                            binding.progressLoading.visibility = View.GONE
                            latestRecords = state.records
                            renderSummary(state.records)
                            renderWeeklyChart(state.records)
                        }
                        is SleepUiState.Error -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.textError.visibility = View.VISIBLE
                            binding.textError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun renderSummary(records: List<SleepRecordDto>) {
        val mostRecent = records.maxByOrNull { it.sleepStart }

        if (mostRecent == null) {
            binding.textTotalSleep.text = "0h 00m"
            binding.textQualityScore.text = "0 / 100"
            binding.textQualityLabel.text = "No data"
            binding.sleepRing.setProgress(0f)
            return
        }

        val hours = mostRecent.totalDurationMinutes / 60
        val minutes = mostRecent.totalDurationMinutes % 60
        binding.textTotalSleep.text = "${hours}h ${minutes.toString().padStart(2, '0')}m"

        binding.textQualityScore.text = "${mostRecent.qualityScore} / 100"
        binding.textQualityLabel.text = qualityLabel(mostRecent.qualityRating)

        val progress = mostRecent.totalDurationMinutes.toFloat() / targetSleepMinutes.toFloat()
        binding.sleepRing.setProgress(progress)
    }

    private fun renderWeeklyChart(records: List<SleepRecordDto>) {
        val hoursPerDay = FloatArray(7) // index 0 = Monday
        val calendar = Calendar.getInstance()

        for (record in records) {
            calendar.timeInMillis = record.sleepStart
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday . 7 = Saturday
            val mondayIndexed = (dayOfWeek + 5) % 7 // 0 = Monday . 6 = Sunday
            hoursPerDay[mondayIndexed] += record.totalDurationMinutes / 60f
        }

        val todayCalendar = Calendar.getInstance()
        val todayDayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)
        val todayIndex = (todayDayOfWeek + 5) % 7

        binding.weeklyBarChart.setData(hoursPerDay.toList(), todayIndex)
    }

    private fun qualityLabel(rating: Int): String = when (rating) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Okay"
        4 -> "Good"
        5 -> "Excellent"
        else -> "No data"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}