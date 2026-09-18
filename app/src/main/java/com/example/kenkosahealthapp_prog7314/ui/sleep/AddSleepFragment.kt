package com.example.kenkosahealthapp_prog7314.ui.sleep

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddSleepBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddSleepFragment : Fragment() {

    private var _binding: FragmentAddSleepBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SleepViewModel by viewModels()

    private val bedtimeCalendar: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 22)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val wakeCalendar: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var selectedQuality: Int = 0
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private lateinit var qualityViews: List<android.widget.TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSleepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        qualityViews = listOf(
            binding.quality1, binding.quality2, binding.quality3, binding.quality4, binding.quality5
        )
        qualityViews.forEachIndexed { index, view ->
            view.setOnClickListener { selectQuality(index + 1) }
        }

        updateTimeLabels()
        updateTotalPreview()

        binding.textBedtime.setOnClickListener { pickTime(bedtimeCalendar) { updateTimeLabels(); updateTotalPreview() } }
        binding.textWakeTime.setOnClickListener { pickTime(wakeCalendar) { updateTimeLabels(); updateTotalPreview() } }

        binding.buttonSaveSleep.setOnClickListener { attemptSave() }
    }

    private fun pickTime(target: Calendar, onSet: () -> Unit) {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                target.set(Calendar.HOUR_OF_DAY, hour)
                target.set(Calendar.MINUTE, minute)
                onSet()
            },
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateTimeLabels() {
        binding.textBedtime.text = timeFormat.format(bedtimeCalendar.time)
        binding.textWakeTime.text = timeFormat.format(wakeCalendar.time)
    }

    private fun updateTotalPreview() {
        var diffMinutes = (wakeCalendar.timeInMillis - bedtimeCalendar.timeInMillis) / 60000
        if (diffMinutes <= 0) diffMinutes += 24 * 60 // overnight rollover
        val hours = diffMinutes / 60
        val minutes = diffMinutes % 60
        binding.textTotalSleepPreview.text = "${hours}h ${minutes.toString().padStart(2, '0')}min"
    }

    private fun selectQuality(rating: Int) {
        selectedQuality = rating
        qualityViews.forEachIndexed { index, view ->
            view.setBackgroundResource(
                if (index + 1 == rating) com.example.kenkosahealthapp_prog7314.R.drawable.bg_quality_selected
                else 0
            )
        }
    }

    private fun attemptSave() {
        if (selectedQuality == 0) {
            showError("Please select a sleep quality")
            return
        }

        val notes = binding.editNotes.text.toString().trim()

        if (notes.length > 500) {
            showError("Notes must be 500 characters or fewer")
            return
        }

        val sleepStartMillis = bedtimeCalendar.timeInMillis
        var sleepEndMillis = wakeCalendar.timeInMillis

        // If wake time is before bedtime, the sleep crossed midnight.
        if (sleepEndMillis <= sleepStartMillis) {
            sleepEndMillis += TimeUnit.DAYS.toMillis(1)
        }

        hideError()
        setLoading(true)

        viewModel.createSleep(
            sleepStart = sleepStartMillis,
            sleepEnd = sleepEndMillis,
            qualityRating = selectedQuality,
            notes = notes.ifEmpty { null },
            onResult = { success, errorMessage ->
                if (_binding == null) return@createSleep

                setLoading(false)

                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(
                        errorMessage
                            ?: "Something went wrong. Please try again."
                    )
                }
            }
        )
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.textError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressSaving.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveSleep.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}