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

/**
 * Add Goal, and (when opened with [ARG_GOAL_ID]) the Edit Goal screen for an existing goal, with a Delete button.
 * Sleep goals are locked to "hours" and 24 at most, because the Sleep screen reads a SLEEP goal's target as hours.
 */
class AddGoalTrackingFragment : Fragment() {

    private var _binding: FragmentAddGoalTrackingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalTrackingViewModel by viewModels()

    private var goalId: String? = null
    private val isEditMode get() = goalId != null

    /** The goal as loaded from the server (edit mode only). */
    private var original: TrackedGoalDto? = null

    private var reminderHour = 6
    private var reminderMinute = 0

    /** The unit we last filled in ourselves, so we only overwrite units the user hasn't typed. */
    private var lastDefaultUnit = ""
    private var isApplyingState = false

    private val categoryChips by lazy {
        mapOf(
            R.id.chipSteps to GoalCategoryOption.STEPS,
            R.id.chipWater to GoalCategoryOption.WATER,
            R.id.chipSleep to GoalCategoryOption.SLEEP,
            R.id.chipExercise to GoalCategoryOption.EXERCISE,
            R.id.chipCustom to GoalCategoryOption.CUSTOM
        )
    }

    private val frequencyChips by lazy {
        mapOf(
            R.id.chipDaily to GoalProgress.DAILY,
            R.id.chipWeekly to GoalProgress.WEEKLY,
            R.id.chipMonthly to GoalProgress.MONTHLY
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGoalTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goalId = arguments?.getString(ARG_GOAL_ID)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, _ ->
            if (!isApplyingState) onCategoryChanged()
        }
        binding.rowReminderTime.setOnClickListener { pickReminderTime() }
        binding.buttonSave.setOnClickListener { attemptSave() }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        updateReminderLabel()

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
            navGoals.setOnClickListener { findNavController().popBackStack(R.id.goalsFragment, false) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addGoal_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addGoal_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    // ------------------------------------------------------------- category

    private fun selectedCategory(): GoalCategoryOption =
        categoryChips[binding.chipGroupCategory.checkedChipId] ?: GoalCategoryOption.STEPS

    private fun selectedFrequency(): String =
        frequencyChips[binding.chipGroupFrequency.checkedChipId] ?: GoalProgress.DAILY

    /** Fills in a sensible unit for the category (unless the user typed their own) and locks it for Sleep. */
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

    private fun parseTime(hhmm: String?): Pair<Int, Int>? {
        val parts = hhmm?.split(":") ?: return null
        val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return hour to minute
    }

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

    private fun updateReminderLabel() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
        }
        binding.textReminderTime.text = DateFormat.getTimeFormat(requireContext()).format(calendar.time)
    }

    // ----------------------------------------------------------------- save

    private fun attemptSave() {
        val name = binding.editGoalName.text.toString().trim()
        val target = binding.editTargetValue.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val unit = binding.editUnit.text.toString().trim()
        val category = selectedCategory()

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
        _binding = null
    }

    companion object {
        const val ARG_GOAL_ID = "goalId"
    }
}
