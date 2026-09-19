package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
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
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentLogMoodBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Log Mood screen. Opened with no arguments it's a blank form; with [ARG_PRESELECT_MOOD] the tapped face is
 * already selected; with [ARG_MOOD_ID] it's the details screen for an existing entry (locked until
 * "Edit Mood" is pressed, and with a Delete button).
 */
class LogMoodFragment : Fragment() {

    private var _binding: FragmentLogMoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels()

    private var moodId: String? = null
    private val isEditMode get() = moodId != null

    /** The entry as loaded from the server (edit mode only). */
    private var original: MoodEntryDto? = null

    private var selectedMood: Mood? = null
    private val selectedTime: Calendar = Calendar.getInstance()
    private var isLocked = false

    private lateinit var moodButtons: Map<Mood, ImageButton>
    private val feelingChips = mutableMapOf<Feeling, Chip>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moodId = arguments?.getString(ARG_MOOD_ID)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupMoodFaces()
        buildFeelingChips()

        updateDateTimeLabels()
        binding.chipDate.setOnClickListener { pickDate() }
        binding.chipTime.setOnClickListener { pickTime() }

        binding.buttonEditMood.setOnClickListener { setLocked(false) }
        binding.buttonSaveMood.setOnClickListener { attemptSave() }
        binding.buttonCancelMood.setOnClickListener { findNavController().navigateUp() }
        binding.buttonDeleteMood.setOnClickListener { confirmDelete() }

        if (isEditMode) {
            binding.textTitle.setText(R.string.mood_details_title)
            binding.buttonEditMood.isEnabled = false // until the entry has loaded
            binding.buttonDeleteMood.isEnabled = false
            binding.buttonDeleteMood.isVisible = true
            setLocked(true)
            observeEditState()
            viewModel.loadMood(moodId!!)
        } else {
            binding.textTitle.setText(R.string.log_mood_title)
            selectedMood = Mood.fromKey(arguments?.getString(ARG_PRESELECT_MOOD))
            renderMoodSelection()
            setLocked(false)
        }
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollLogMood.updatePadding(top = bars.top)
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
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_logMood_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_logMood_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_logMood_to_settings) }
            // fabAdd: already on the log screen.
        }
    }

    private fun setupMoodFaces() {
        moodButtons = mapOf(
            Mood.VERY_HAPPY to binding.moodVeryHappy,
            Mood.HAPPY to binding.moodHappy,
            Mood.NEUTRAL to binding.moodNeutral,
            Mood.SAD to binding.moodSad,
            Mood.VERY_SAD to binding.moodVerySad
        )
        for ((mood, button) in moodButtons) {
            button.setOnClickListener {
                if (isLocked) return@setOnClickListener
                selectedMood = mood
                renderMoodSelection()
            }
        }
    }

    /** Ring the chosen face and dim the others. */
    private fun renderMoodSelection() {
        for ((mood, button) in moodButtons) {
            val chosen = mood == selectedMood
            button.setBackgroundResource(if (chosen) R.drawable.bg_mood_selected else 0)
            button.alpha = if (selectedMood == null || chosen) 1f else 0.45f
            button.isSelected = chosen
        }
    }

    private fun buildFeelingChips() {
        binding.chipGroupFeelings.removeAllViews()
        feelingChips.clear()
        for (feeling in Feeling.values()) {
            val chip = layoutInflater.inflate(
                R.layout.item_feeling_chip, binding.chipGroupFeelings, false
            ) as Chip
            chip.id = View.generateViewId()
            chip.setText(feeling.labelRes)
            chip.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), feeling.colorRes))
            binding.chipGroupFeelings.addView(chip)
            feelingChips[feeling] = chip
        }
    }

    // ------------------------------------------------------------ edit mode

    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is MoodEditState.Idle -> Unit
                        is MoodEditState.Loading -> setLoading(true)
                        is MoodEditState.Loaded -> {
                            setLoading(false)
                            hideError()
                            // Only fill the form once per view, so coming back from the background
                            // doesn't wipe edits the user hasn't saved yet.
                            if (original == null) populate(state.entry)
                        }
                        is MoodEditState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun populate(entry: MoodEntryDto) {
        original = entry

        selectedMood = Mood.fromKey(entry.mood)
        renderMoodSelection()

        for ((feeling, chip) in feelingChips) chip.isChecked = feeling.name in entry.feelings

        selectedTime.timeInMillis = entry.loggedAt
        updateDateTimeLabels()

        binding.buttonEditMood.isEnabled = true
        binding.buttonDeleteMood.isEnabled = true
    }

    /** Locked = read-only details view; unlocked = editable form. */
    private fun setLocked(locked: Boolean) {
        isLocked = locked

        binding.chipDate.isEnabled = !locked
        binding.chipTime.isEnabled = !locked
        for (button in moodButtons.values) button.isEnabled = !locked
        for (chip in feelingChips.values) chip.isEnabled = !locked

        binding.buttonEditMood.isVisible = locked
        binding.buttonSaveMood.isVisible = !locked
    }

    // ------------------------------------------------------------ date/time

    private fun updateDateTimeLabels() {
        val millis = selectedTime.timeInMillis
        binding.chipDate.text = DateUtils.formatDateTime(
            requireContext(), millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )
        binding.chipTime.text = DateUtils.formatDateTime(requireContext(), millis, DateUtils.FORMAT_SHOW_TIME)
    }

    private fun pickDate() {
        if (isLocked) return
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedTime.set(year, month, day)
                updateDateTimeLabels()
            },
            selectedTime.get(Calendar.YEAR),
            selectedTime.get(Calendar.MONTH),
            selectedTime.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // no future moods
        }.show()
    }

    private fun pickTime() {
        if (isLocked) return
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedTime.set(Calendar.MINUTE, minute)
                selectedTime.set(Calendar.SECOND, 0)
                selectedTime.set(Calendar.MILLISECOND, 0)
                updateDateTimeLabels()
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    // ----------------------------------------------------------------- save

    private fun attemptSave() {
        val mood = selectedMood ?: return showError(getString(R.string.mood_error_select))

        // One minute of slack so "now" never trips the check.
        if (selectedTime.timeInMillis > System.currentTimeMillis() + 60_000L) {
            return showError(getString(R.string.mood_error_future))
        }

        val feelings = Feeling.values()
            .filter { feelingChips[it]?.isChecked == true }
            .map { it.name }
        val loggedAt = selectedTime.timeInMillis

        hideError()
        setLoading(true)

        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            if (_binding != null) {
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: getString(R.string.mood_error_generic))
                }
            }
        }

        val id = moodId
        if (id == null) {
            viewModel.createMood(mood.name, feelings, loggedAt, onResult)
        } else {
            viewModel.updateMood(id, mood.name, feelings, loggedAt, onResult)
        }
    }

    private fun confirmDelete() {
        val id = moodId ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mood_delete_title)
            .setMessage(R.string.mood_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                setLoading(true)
                viewModel.deleteMood(id) { success, errorMessage ->
                    if (_binding == null) return@deleteMood
                    setLoading(false)
                    if (success) {
                        findNavController().navigateUp()
                    } else {
                        showError(errorMessage ?: getString(R.string.mood_error_generic))
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
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
        binding.buttonSaveMood.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        original = null // the next view gets filled from the ViewModel again
        _binding = null
    }

    companion object {
        const val ARG_MOOD_ID = "moodId"
        const val ARG_PRESELECT_MOOD = "preselectMood"
    }
}
