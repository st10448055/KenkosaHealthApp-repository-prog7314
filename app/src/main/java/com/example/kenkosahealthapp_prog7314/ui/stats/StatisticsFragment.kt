package com.example.kenkosahealthapp_prog7314.ui.stats

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.DayStatsDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

/**
 * Last 7 days of water, sleep and calories, loaded in one call from GET api/stats/weekly.
 * The server groups the data by the phone's time zone so "today" matches what the user sees.
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        // The app is edge-to-edge, so keep content clear of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.scrollStats.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.cardWater.textTitle.setText(R.string.stats_water)
        binding.cardSleep.textTitle.setText(R.string.stats_sleep)
        binding.cardCalories.textTitle.setText(R.string.stats_calories)

        loadWeek()
    }

    private fun loadWeek() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            binding.textError.visibility = View.GONE
            try {
                val days = RetrofitInstance.statsApi.weekly(TimeZone.getDefault().id)
                showWeek(days)
                Log.d(TAG, "Loaded ${days.size} days of stats")
            } catch (e: Exception) {
                Log.e(TAG, "Stats load failed", e)
                binding.textError.visibility = View.VISIBLE
                binding.textError.setText(R.string.stats_error)
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showWeek(days: List<DayStatsDto>) {
        if (days.isEmpty()) return
        val today = days.lastIndex
        val locale = Locale.getDefault()

        val cups = days.map { it.cups.toFloat() }
        val hours = days.map { it.sleepMinutes / 60f }
        val kcal = days.map { it.calories.toFloat() }

        binding.cardWater.chart.setData(cups, today)
        binding.cardSleep.chart.setData(hours, today)
        binding.cardCalories.chart.setData(kcal, today)

        fillLabels(binding.cardWater.labels, days)
        fillLabels(binding.cardSleep.labels, days)
        fillLabels(binding.cardCalories.labels, days)

        binding.cardWater.textAvg.text =
            getString(R.string.stats_avg_water, String.format(locale, "%.1f", cups.average()))
        binding.cardSleep.textAvg.text =
            getString(R.string.stats_avg_sleep, String.format(locale, "%.1f", hours.average()))
        binding.cardCalories.textAvg.text =
            getString(R.string.stats_avg_calories, String.format(locale, "%,.0f", kcal.average()))
    }

    /** One narrow weekday letter per bar, in the app's current language. */
    private fun fillLabels(container: LinearLayout, days: List<DayStatsDto>) {
        container.removeAllViews()
        val color = ContextCompat.getColor(requireContext(), R.color.settings_text_secondary)
        days.forEach { day ->
            container.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(color)
                text = LocalDate.parse(day.date).dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "StatisticsFragment"
    }
}