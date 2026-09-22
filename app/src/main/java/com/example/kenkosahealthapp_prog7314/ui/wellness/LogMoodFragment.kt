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

    // View Binding gives direct access to the views in the Fragment layout.
    // It is cleared when the Fragment's view is destroyed.
    private var _binding: FragmentLogMoodBinding? = null
    private val binding get() = _binding!!

    // The ViewModel manages loading, creating, updating and deleting mood entries.
    private val viewModel: MoodViewModel by viewModels()

    // Stores the ID when viewing an existing mood entry.
    private var moodId: String? = null

    // If there is a mood ID, this screen is being used for an existing entry.
    private val isEditMode get() = moodId != null

    // Stores the original entry loaded from the server in edit mode.
    private var original: MoodEntryDto? = null

    // Keeps track of the mood currently selected by the user.
    private var selectedMood: Mood? = null

    // Stores the date and time selected for the mood entry.
    private val selectedTime: Calendar = Calendar.getInstance()

    // Controls whether the form is currently read-only.
    private var isLocked = false

    // Maps each Mood value to its corresponding ImageButton.
    private lateinit var moodButtons: Map<Mood, ImageButton>

    // Stores the dynamically created feeling chips.
    private val feelingChips = mutableMapOf<Feeling, Chip>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using View Binding.
        _binding = FragmentLogMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check whether this Fragment was opened with an existing mood ID.
        moodId = arguments?.getString(ARG_MOOD_ID)

        // Set up the different parts of the screen.
        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupMoodFaces()
        buildFeelingChips()

        // Display the current date and time when the screen opens.
        updateDateTimeLabels()

        // Allow the user to change the selected date or time.
        binding.chipDate.setOnClickListener { pickDate() }
        binding.chipTime.setOnClickListener { pickTime() }

        // Edit unlocks an existing mood entry so it can be changed.
        binding.buttonEditMood.setOnClickListener { setLocked(false) }

        // Save validates the form and sends the data through the ViewModel.
        binding.buttonSaveMood.setOnClickListener { attemptSave() }

        // Cancel returns to the previous screen without saving.
        binding.buttonCancelMood.setOnClickListener { findNavController().navigateUp() }

        // Delete asks for confirmation before removing an existing mood.
        binding.buttonDeleteMood.setOnClickListener { confirmDelete() }

        if (isEditMode) {
            // Existing entries are initially displayed as a read-only details screen.
            binding.textTitle.setText(R.string.mood_details_title)

            // Disable edit and delete until the existing entry has loaded.
            binding.buttonEditMood.isEnabled = false
            binding.buttonDeleteMood.isEnabled = false
            binding.buttonDeleteMood.isVisible = true

            setLocked(true)

            // Listen for the ViewModel to finish loading the mood entry.
            observeEditState()
            viewModel.loadMood(moodId!!)
        } else {
            // New mood entries start with an empty editable form.
            binding.textTitle.setText(R.string.log_mood_title)

            // A mood can optionally be pre-selected when navigating to this screen.
            selectedMood = Mood.fromKey(arguments?.getString(ARG_PRESELECT_MOOD))

            renderMoodSelection()
            setLocked(false)
        }
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        // Adjust the screen's padding so content does not overlap the system status
        // bar, display cutout or navigation bar.
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
        // Open the navigation drawer through the Home Fragment.
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()

            // Pass a flag to HomeFragment telling it to open the drawer.
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment)
                    .savedStateHandle
                    .set(HomeFragment.KEY_OPEN_DRAWER, true)
            }

            // Return to Home after requesting the drawer to open.
            nav.popBackStack(R.id.homeFragment, false)
        }

        // The notification bell is intentionally non-functional for now.
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {

            // Return to the Home screen.
            navHome.setOnClickListener {
                findNavController().popBackStack(R.id.homeFragment, false)
            }

            // Navigate to the Goals screen.
            navGoals.setOnClickListener {
                findNavController().navigate(R.id.action_logMood_to_goals)
            }

            // Navigate to the Statistics screen.
            navStats.setOnClickListener {
                findNavController().navigate(R.id.action_logMood_to_statistics)
            }

            // Navigate to Settings.
            navSettings.setOnClickListener {
                findNavController().navigate(R.id.action_logMood_to_settings)
            }

            // fabAdd is not needed because this screen is already used to log a mood.
        }
    }

    private fun setupMoodFaces() {
        // Connect each Mood value to the ImageButton representing it in the layout.
        moodButtons = mapOf(
            Mood.VERY_HAPPY to binding.moodVeryHappy,
            Mood.HAPPY to binding.moodHappy,
            Mood.NEUTRAL to binding.moodNeutral,
            Mood.SAD to binding.moodSad,
            Mood.VERY_SAD to binding.moodVerySad
        )

        // Add a click listener to each mood face.
        for ((mood, button) in moodButtons) {
            button.setOnClickListener {

                // Do not allow mood changes while viewing a locked entry.
                if (isLocked) return@setOnClickListener

                selectedMood = mood
                renderMoodSelection()
            }
        }
    }

    // Highlight the selected mood and dim the other mood faces.
    private fun renderMoodSelection() {
        for ((mood, button) in moodButtons) {
            val chosen = mood == selectedMood

            // Apply a background to the selected face.
            button.setBackgroundResource(
                if (chosen) R.drawable.bg_mood_selected else 0
            )

            // Dim unselected faces to make the selected mood stand out.
            button.alpha = if (selectedMood == null || chosen) 1f else 0.45f

            button.isSelected = chosen
        }
    }

    private fun buildFeelingChips() {
        // Clear existing chips before rebuilding the list.
        binding.chipGroupFeelings.removeAllViews()
        feelingChips.clear()

        // Create a Chip for every available Feeling.
        for (feeling in Feeling.values()) {
            val chip = layoutInflater.inflate(
                R.layout.item_feeling_chip,
                binding.chipGroupFeelings,
                false
            ) as Chip

            // Give each dynamically created Chip its own unique ID.
            chip.id = View.generateViewId()

            // Set the text and colour based on the Feeling definition.
            chip.setText(feeling.labelRes)
            chip.chipBackgroundColor =
                ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), feeling.colorRes)
                )

            // Add the Chip to the layout and keep a reference to it.
            binding.chipGroupFeelings.addView(chip)
            feelingChips[feeling] = chip
        }
    }

    // ------------------------------------------------------------ edit mode

    private fun observeEditState() {
        // Observe the ViewModel while the Fragment's view is in the STARTED state.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is MoodEditState.Idle -> Unit

                        is MoodEditState.Loading -> setLoading(true)

                        is MoodEditState.Loaded -> {
                            setLoading(false)
                            hideError()

                            // Only populate the form the first time the data is loaded.
                            // This prevents state updates from overwriting unsaved edits.
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
        // Save the original entry so we know that the form has already been populated.
        original = entry

        // Restore the mood selected when the entry was originally saved.
        selectedMood = Mood.fromKey(entry.mood)
        renderMoodSelection()

        // Restore all previously selected feelings.
        for ((feeling, chip) in feelingChips) {
            chip.isChecked = feeling.name in entry.feelings
        }

        // Restore the original date and time.
        selectedTime.timeInMillis = entry.loggedAt
        updateDateTimeLabels()

        // The entry is now fully loaded, so editing and deleting can be enabled.
        binding.buttonEditMood.isEnabled = true
        binding.buttonDeleteMood.isEnabled = true
    }

    // Locked means the screen is read-only; unlocked allows the user to edit the entry.
    private fun setLocked(locked: Boolean) {
        isLocked = locked

        // Enable or disable the date, time, mood and feeling controls.
        binding.chipDate.isEnabled = !locked
        binding.chipTime.isEnabled = !locked

        for (button in moodButtons.values) {
            button.isEnabled = !locked
        }

        for (chip in feelingChips.values) {
            chip.isEnabled = !locked
        }

        // Show Edit while locked and Save while editing.
        binding.buttonEditMood.isVisible = locked
        binding.buttonSaveMood.isVisible = !locked
    }

    // ------------------------------------------------------------ date/time

    private fun updateDateTimeLabels() {
        val millis = selectedTime.timeInMillis

        // Format the selected timestamp into a readable date.
        binding.chipDate.text = DateUtils.formatDateTime(
            requireContext(),
            millis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_YEAR or
                    DateUtils.FORMAT_ABBREV_MONTH
        )

        // Format the selected timestamp into a readable time.
        binding.chipTime.text = DateUtils.formatDateTime(
            requireContext(),
            millis,
            DateUtils.FORMAT_SHOW_TIME
        )
    }

    private fun pickDate() {
        if (isLocked) return

        // Open a date picker using the currently selected date.
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->

                // Update the Calendar when the user chooses a new date.
                selectedTime.set(year, month, day)
                updateDateTimeLabels()
            },
            selectedTime.get(Calendar.YEAR),
            selectedTime.get(Calendar.MONTH),
            selectedTime.get(Calendar.DAY_OF_MONTH)
        ).apply {

            // Prevent the user from logging a mood in the future.
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun pickTime() {
        if (isLocked) return

        // Open a time picker using the currently selected time.
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->

                // Update only the time portion of the Calendar.
                selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedTime.set(Calendar.MINUTE, minute)
                selectedTime.set(Calendar.SECOND, 0)
                selectedTime.set(Calendar.MILLISECOND, 0)

                updateDateTimeLabels()
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),

            // Follow the user's device setting for 12-hour or 24-hour time.
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    // ----------------------------------------------------------------- save

    private fun attemptSave() {

        // A mood must be selected before the entry can be saved.
        val mood = selectedMood
            ?: return showError(getString(R.string.mood_error_select))

        // Prevent future timestamps from being saved.
        // The one-minute allowance prevents small timing differences from causing an error.
        if (selectedTime.timeInMillis > System.currentTimeMillis() + 60_000L) {
            return showError(getString(R.string.mood_error_future))
        }

        // Collect the names of all feelings that the user selected.
        val feelings = Feeling.values()
            .filter { feelingChips[it]?.isChecked == true }
            .map { it.name }

        val loggedAt = selectedTime.timeInMillis

        hideError()
        setLoading(true)

        // Callback used after the ViewModel finishes creating or updating the entry.
        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            if (_binding != null) {
                setLoading(false)

                if (success) {
                    // Return to the previous screen after a successful save.
                    findNavController().navigateUp()
                } else {
                    showError(
                        errorMessage ?: getString(R.string.mood_error_generic)
                    )
                }
            }
        }

        // If there is no ID, create a new entry.
        // Otherwise, update the existing entry.
        val id = moodId

        if (id == null) {
            viewModel.createMood(
                mood.name,
                feelings,
                loggedAt,
                onResult
            )
        } else {
            viewModel.updateMood(
                id,
                mood.name,
                feelings,
                loggedAt,
                onResult
            )
        }
    }

    private fun confirmDelete() {
        val id = moodId ?: return

        // Show a confirmation dialog before deleting the mood entry.
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mood_delete_title)
            .setMessage(R.string.mood_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->

                setLoading(true)

                // Ask the ViewModel to delete the selected entry.
                viewModel.deleteMood(id) { success, errorMessage ->
                    if (_binding == null) return@deleteMood

                    setLoading(false)

                    if (success) {
                        // Return to the previous screen after successful deletion.
                        findNavController().navigateUp()
                    } else {
                        showError(
                            errorMessage ?: getString(R.string.mood_error_generic)
                        )
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showError(message: String) {
        // Display an error message in the UI.
        binding.textError.text = message
        binding.textError.isVisible = true
    }

    private fun hideError() {
        // Hide the error message when there is no current error.
        binding.textError.isVisible = false
    }

    private fun setLoading(loading: Boolean) {
        // Show or hide the saving/loading indicator.
        binding.progressSaving.isVisible = loading

        // Prevent multiple save clicks while an operation is running.
        binding.buttonSaveMood.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clear the original entry so a newly created view is populated again.
        original = null

        // Release the binding when the Fragment's view is destroyed.
        _binding = null
    }

    companion object {
        // Argument used when opening an existing mood entry.
        const val ARG_MOOD_ID = "moodId"

        // Argument used to automatically select a mood when opening the form.
        const val ARG_PRESELECT_MOOD = "preselectMood"
    }
}