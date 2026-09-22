package com.example.kenkosahealthapp_prog7314.ui.goals

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.kenkosahealthapp_prog7314.data.network.SaveTrackedGoalRequest
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddGoalTrackingBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// Fragment for creating a new goal or editing/deleting an existing goal.
// Sleep goals are restricted to "hours" and capped at 24.
class AddGoalTrackingFragment : Fragment() {

    // Backing variable for binding layout instance
    private var _binding: FragmentAddGoalTrackingBinding? = null

    // Non-null binding property valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    // ViewModel handling goal persistence and business logic
    private val viewModel: GoalTrackingViewModel by viewModels()

    // Identifier passed when editing an existing goal
    private var goalId: String? = null

    // Flag indicating whether the screen is in Edit mode
    private val isEditMode get() = goalId != null

    // Original goal entity fetched from server when editing
    private var original: TrackedGoalDto? = null

    // Stored hour for reminder picker (default 6 AM)
    private var reminderHour = 6

    // Stored minute for reminder picker (default 0)
    private var reminderMinute = 0

    // Stores unit autofilled by category selection to avoid overwriting user edits
    private var lastDefaultUnit = ""

    // Flag preventing programmatic updates from triggering UI change listeners
    private var isApplyingState = false

    // Maps chip view IDs to category enum options
    private val categoryChips by lazy {
        mapOf(
            R.id.chipSteps to GoalCategoryOption.STEPS,
            R.id.chipWater to GoalCategoryOption.WATER,
            R.id.chipSleep to GoalCategoryOption.SLEEP,
            R.id.chipExercise to GoalCategoryOption.EXERCISE,
            R.id.chipCustom to GoalCategoryOption.CUSTOM
        )
    }

    // Maps chip view IDs to goal frequency string constants
    private val frequencyChips by lazy {
        mapOf(
            R.id.chipDaily to GoalProgress.DAILY,
            R.id.chipWeekly to GoalProgress.WEEKLY,
            R.id.chipMonthly to GoalProgress.MONTHLY
        )
    }

    // Inflates the layout and initializes binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGoalTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Configures views, event listeners, and initial state upon view creation
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract passed goal ID argument
        goalId = arguments?.getString(ARG_GOAL_ID)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        // Handle category chip changes
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, _ ->
            if (!isApplyingState) onCategoryChanged()
        }

        // Action button click handlers
        binding.rowReminderTime.setOnClickListener { pickReminderTime() }
        binding.buttonSave.setOnClickListener { attemptSave() }
        binding.buttonCancel.setOnClickListener { findNavController().navigateUp() }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        updateReminderLabel()

        // Configure layout based on mode (Edit vs Add)
        if (isEditMode) {
            binding.textTitle.setText(R.string.goaltrack_edit_title)
            binding.buttonSave.setText(R.string.goaltrack_save_changes)
            binding.buttonSave.isEnabled = false // until the goal has loaded
            binding.buttonDelete.isEnabled = false
            binding.buttonDelete.isVisible = true
            observeEditState()
            viewModel.loadGoal(goalId!!)
        } else {
            binding.textTitle.setText(R.string.goaltrack_add_title)
            binding.buttonSave.setText(R.string.goaltrack_create)
            onCategoryChanged() // Steps is checked by default: fill in its unit
        }
    }

    // ---------------------------------------------------------------- setup
// Adjusts padding to prevent UI from hiding behind system bars and notch cutouts
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollAddGoal.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    // Sets up header actions and navigation drawer trigger
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

    // Sets up click listeners for bottom navigation bar items
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().popBackStack(R.id.goalsFragment, false) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addGoal_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addGoal_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    // ------------------------------------------------------------- category
    // Returns currently selected category from chip group
    private fun selectedCategory(): GoalCategoryOption =
        categoryChips[binding.chipGroupCategory.checkedChipId] ?: GoalCategoryOption.STEPS

    // Returns currently selected frequency string from chip group
    private fun selectedFrequency(): String =
        frequencyChips[binding.chipGroupFrequency.checkedChipId] ?: GoalProgress.DAILY

    // Autofills unit field for chosen category and locks editing if category is Sleep
    private fun onCategoryChanged() {
        val category = selectedCategory()
        val newDefault = category.defaultUnitRes?.let { getString(it) } ?: ""
        val currentUnit = binding.editUnit.text.toString().trim()

        if (currentUnit.isEmpty() || currentUnit == lastDefaultUnit || category == GoalCategoryOption.SLEEP) {
            binding.editUnit.setText(newDefault)
        }
        lastDefaultUnit = newDefault
        binding.editUnit.isEnabled = category != GoalCategoryOption.SLEEP
    }

    // ------------------------------------------------------------ edit mode
    // Observes goal edit state changes from ViewModel
    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is GoalEditState.Idle -> Unit
                        is GoalEditState.Loading -> setLoading(true)
                        is GoalEditState.Loaded -> {
                            setLoading(false)
                            hideError()
                            // Only fill the form once per view, so coming back from the background
                            // doesn't wipe edits the user hasn't saved yet.
                            if (original == null) populate(state.goal)
                        }
                        is GoalEditState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }
    // Fills input fields with existing goal values when in Edit mode
    private fun populate(goal: TrackedGoalDto) {
        original = goal

        isApplyingState = true
        val category = GoalCategoryOption.fromKey(goal.category)
        binding.chipGroupCategory.check(categoryChips.entries.first { it.value == category }.key)
        binding.chipGroupFrequency.check(
            frequencyChips.entries.firstOrNull { it.value == goal.frequency }?.key ?: R.id.chipDaily
        )
        isApplyingState = false

        binding.editGoalName.setText(goal.name)
        binding.editTargetValue.setText(
            if (goal.targetValue == goal.targetValue.toLong().toDouble()) goal.targetValue.toLong().toString()
            else goal.targetValue.toString()
        )
        binding.editUnit.setText(goal.unit)
        binding.editUnit.isEnabled = category != GoalCategoryOption.SLEEP
        lastDefaultUnit = category.defaultUnitRes?.let { getString(it) } ?: ""

        binding.switchReminders.isChecked = goal.remindersEnabled
        parseTime(goal.reminderTime)?.let { (hour, minute) ->
            reminderHour = hour
            reminderMinute = minute
        }
        updateReminderLabel()

        binding.buttonSave.isEnabled = true
        binding.buttonDelete.isEnabled = true
    }

    // ------------------------------------------------------------- reminder
    // Parses HH:mm string into hour and minute integers
    private fun parseTime(hhmm: String?): Pair<Int, Int>? {
        val parts = hhmm?.split(":") ?: return null
        val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return hour to minute
    }

    // Displays time picker dialog for reminder selection
    private fun pickReminderTime() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                updateReminderLabel()
            },
            reminderHour,
            reminderMinute,
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    // Updates reminder time text label according to user time format preferences
    private fun updateReminderLabel() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
        }
        binding.textReminderTime.text = DateFormat.getTimeFormat(requireContext()).format(calendar.time)
    }

    // ----------------------------------------------------------------- save
    // Validates form input and triggers goal creation or update request
    private fun attemptSave() {
        val name = binding.editGoalName.text.toString().trim()
        val target = binding.editTargetValue.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val unit = binding.editUnit.text.toString().trim()
        val category = selectedCategory()

        // Form validations
        if (name.isEmpty()) return showError(getString(R.string.goaltrack_error_name))
        if (name.length > 60) return showError(getString(R.string.goaltrack_error_name_length))
        if (target == null || target <= 0) return showError(getString(R.string.goaltrack_error_target))
        if (target > 1_000_000) return showError(getString(R.string.goaltrack_error_target_large))
        if (unit.isEmpty()) return showError(getString(R.string.goaltrack_error_unit))
        if (category == GoalCategoryOption.SLEEP && target > 24) {
            return showError(getString(R.string.goaltrack_error_sleep))
        }

        val request = SaveTrackedGoalRequest(
            category = category.name,
            name = name,
            targetValue = target,
            unit = unit,
            frequency = selectedFrequency(),
            remindersEnabled = binding.switchReminders.isChecked,
            reminderTime = String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute)
        )

        hideError()
        setLoading(true)

        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            if (_binding != null) {
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: getString(R.string.goaltrack_error_generic))
                }
            }
        }

        val id = goalId
        if (id == null) viewModel.createGoal(request, onResult) else viewModel.updateGoal(id, request, onResult)
    }

    // Displays confirmation dialog before deleting goal
    private fun confirmDelete() {
        val id = goalId ?: return
        val name = original?.name ?: binding.editGoalName.text.toString()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.goaltrack_delete_title)
            .setMessage(getString(R.string.goaltrack_delete_message, name))
            .setPositiveButton(R.string.goaltrack_delete) { _, _ ->
                setLoading(true)
                viewModel.deleteGoal(id) { success, errorMessage ->
                    if (_binding == null) return@deleteGoal
                    setLoading(false)
                    if (success) {
                        findNavController().navigateUp()
                    } else {
                        showError(errorMessage ?: getString(R.string.goaltrack_error_generic))
                    }
                }
            }
            .setNegativeButton(R.string.goaltrack_cancel, null)
            .show()
    }

    // Shows error message UI label
    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.isVisible = true
    }

    // Hides error message UI label
    private fun hideError() {
        binding.textError.isVisible = false
    }

    // Sets save loading state and enables/disables save button
    private fun setLoading(loading: Boolean) {
        binding.progressSaving.isVisible = loading
        binding.buttonSave.isEnabled = !loading
    }

    // Cleans up binding reference when view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        original = null // the next view gets filled from the ViewModel again
        _binding = null
    }

    companion object {
        // Navigation argument key for passing existing goal ID
        const val ARG_GOAL_ID = "goalId"
    }
}
//---------------------------------------END OF FILE-------------------------------------------------