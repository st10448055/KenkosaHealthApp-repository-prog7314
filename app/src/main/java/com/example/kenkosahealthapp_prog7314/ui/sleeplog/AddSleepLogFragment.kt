package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.SaveSleepLogRequest
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddSleepLogBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// Add Sleep, and (when opened with ARG_SLEEP_ID) the Edit Sleep screen for an existing night, with a Delete button.
//
// You enter a bedtime and a wake-up time (and the wake-up date, which defaults to today). If the bedtime is later in
// the day than the wake-up time, the night started the evening before, so 10:30 PM -> 6:30 AM is 8 hours.
class AddSleepLogFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentAddSleepLogBinding? = null
    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private val viewModel: SleepLogViewModel by viewModels()

    private var sleepId: String? = null
    private val isEditMode get() = sleepId != null

    // The record as loaded from the server (edit mode only).
    private var original: SleepLogDto? = null

    private var wakeDate: LocalDate = LocalDate.now()
    private var bedTime: LocalTime = LocalTime.of(22, 30)
    private var wakeTime: LocalTime = LocalTime.of(6, 30)

    // 1 (Poor) to 5 (Excellent); 0 = not chosen yet.
    private var quality = 0

    private lateinit var faceButtons: List<ImageButton>

    private val zone: ZoneId get() = ZoneId.systemDefault()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSleepLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sleepId = arguments?.getString(ARG_SLEEP_ID)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupFaces()

        binding.rowBedtime.setOnClickListener { pickTime(bedTime) { bedTime = it } }
        binding.rowWakeTime.setOnClickListener { pickTime(wakeTime) { wakeTime = it } }
        binding.chipWakeDate.setOnClickListener { pickDate() }
        binding.buttonSave.setOnClickListener { attemptSave() }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        updateLabels()
        renderFaces()

        if (isEditMode) {
            binding.textTitle.setText(R.string.sleeplog_edit_title)
            binding.buttonSave.setText(R.string.sleeplog_save_changes)
            binding.buttonSave.isEnabled = false // until the record has loaded
            binding.buttonDelete.isEnabled = false
            binding.buttonDelete.isVisible = true
            observeEditState()
            viewModel.loadRecord(sleepId!!)
        } else {
            binding.textTitle.setText(R.string.sleeplog_add_title)
            binding.buttonSave.setText(R.string.sleeplog_save)
        }
    }

    // ---------------------------------------------------------------- setup

    // Applies system bar insets as padding so content isn't drawn under the status/nav bars
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollAddSleep.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupHeader() {
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_addSleep_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addSleep_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addSleep_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    private fun setupFaces() {
        faceButtons = listOf(binding.faceQ1, binding.faceQ2, binding.faceQ3, binding.faceQ4, binding.faceQ5)
        faceButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                quality = index + 1
                renderFaces()
            }
        }
    }

    // Ring the chosen face and dim the others.
    private fun renderFaces() {
        faceButtons.forEachIndexed { index, button ->
            val chosen = quality == index + 1
            button.setBackgroundResource(if (chosen) R.drawable.bg_sleeplog_face_selected else 0)
            button.alpha = if (quality == 0 || chosen) 1f else 0.45f
        }
    }

    // ------------------------------------------------------------ edit mode

    // Observes the record load state and fills the form once loading succeeds
    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is SleepEditState.Idle -> Unit
                        is SleepEditState.Loading -> setLoading(true)
                        is SleepEditState.Loaded -> {
                            setLoading(false)
                            hideError()
                            // Only fill the form once per view, so coming back from the background
                            // doesn't wipe edits the user hasn't saved yet.
                            if (original == null) populate(state.record)
                        }
                        is SleepEditState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    // Fills all form fields from a loaded sleep record, converting epoch millis to local date/time
    private fun populate(record: SleepLogDto) {
        original = record

        val start = Instant.ofEpochMilli(record.sleepStart).atZone(zone)
        val end = Instant.ofEpochMilli(record.sleepEnd).atZone(zone)
        wakeDate = end.toLocalDate()
        bedTime = start.toLocalTime().withSecond(0).withNano(0)
        wakeTime = end.toLocalTime().withSecond(0).withNano(0)
        quality = record.qualityRating.coerceIn(1, 5)

        binding.editNotes.setText(record.notes.orEmpty())
        updateLabels()
        renderFaces()

        binding.buttonSave.isEnabled = true
        binding.buttonDelete.isEnabled = true
    }

    // ------------------------------------------------------------ date/time

    // Start and end as epoch millis, or null if bedtime and wake-up time are the same.
    private fun startAndEnd(): Pair<Long, Long>? {
        if (bedTime == wakeTime) return null
        val end = LocalDateTime.of(wakeDate, wakeTime)
        // If bedtime is later in the day than wake time, the night started the day before
        val startDate = if (bedTime.isAfter(wakeTime)) wakeDate.minusDays(1) else wakeDate
        val start = LocalDateTime.of(startDate, bedTime)
        return start.atZone(zone).toInstant().toEpochMilli() to end.atZone(zone).toInstant().toEpochMilli()
    }

    // Formats a LocalTime using the device's 12/24-hour system setting
    private fun formatTime(time: LocalTime): String =
        DateFormat.getTimeFormat(requireContext()).format(
            Date.from(time.atDate(LocalDate.now()).atZone(zone).toInstant())
        )

    // Refreshes all the displayed labels (bedtime, wake time, wake date, total sleep) from current state
    private fun updateLabels() {
        binding.textBedtime.text = formatTime(bedTime)
        binding.textWakeTime.text = formatTime(wakeTime)
        binding.chipWakeDate.text = DateUtils.formatDateTime(
            requireContext(),
            wakeDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )

        val range = startAndEnd()
        binding.textTotalSleep.text = if (range == null) {
            getString(R.string.sleeplog_none)
        } else {
            SleepMath.formatLong(requireContext(), ((range.second - range.first) / 60_000L).toInt())
        }
    }

    private fun pickTime(current: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                onPicked(LocalTime.of(hour, minute))
                updateLabels()
            },
            current.hour,
            current.minute,
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    private fun pickDate() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                wakeDate = LocalDate.of(year, month + 1, day)
                updateLabels()
            },
            wakeDate.year,
            wakeDate.monthValue - 1,
            wakeDate.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // no sleeps that haven't happened yet
        }.show()
    }

    // ----------------------------------------------------------------- save

    // Validates the entered times/quality and calls create or update depending on mode
    private fun attemptSave() {
        val range = startAndEnd() ?: return showError(getString(R.string.sleeplog_error_same_time))
        if (quality == 0) return showError(getString(R.string.sleeplog_error_quality))
        // One minute of slack so "just now" never trips the check.
        if (range.second > System.currentTimeMillis() + 60_000L) {
            return showError(getString(R.string.sleeplog_error_future))
        }

        val request = SaveSleepLogRequest(
            sleepStart = range.first,
            sleepEnd = range.second,
            qualityRating = quality,
            notes = binding.editNotes.text.toString().trim().ifEmpty { null }
        )

        hideError()
        setLoading(true)

        // Shared callback for both create and update, since they handle success/failure identically
        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            if (_binding != null) {
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: getString(R.string.sleeplog_error_generic))
                }
            }
        }

        val id = sleepId
        if (id == null) viewModel.createSleep(request, onResult) else viewModel.updateSleep(id, request, onResult)
    }

    // Shows a confirmation dialog before deleting the current sleep record
    private fun confirmDelete() {
        val id = sleepId ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sleeplog_delete_title)
            .setMessage(R.string.sleeplog_delete_message)
            .setPositiveButton(R.string.sleeplog_delete) { _, _ ->
                setLoading(true)
                viewModel.deleteSleep(id) { success, errorMessage ->
                    if (_binding == null) return@deleteSleep
                    setLoading(false)
                    if (success) {
                        findNavController().navigateUp()
                    } else {
                        showError(errorMessage ?: getString(R.string.sleeplog_error_generic))
                    }
                }
            }
            .setNegativeButton(R.string.sleeplog_cancel, null)
            .show()
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.isVisible = true
    }

    private fun hideError() {
        binding.textError.isVisible = false
    }

    private fun setLoading(loading: Boolean) {
        binding.progressSaving.isVisible = loading
        binding.buttonSave.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        original = null // the next view gets filled from the ViewModel again
        // Prevent memory leaks by releasing the binding reference
        _binding = null
    }

    companion object {
        const val ARG_SLEEP_ID = "sleepId"
    }
}