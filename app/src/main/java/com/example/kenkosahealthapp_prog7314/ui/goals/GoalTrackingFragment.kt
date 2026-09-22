package com.example.kenkosahealthapp_prog7314.ui.goals

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.os.bundleOf
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentGoalTrackingBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemStreakDayBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Displays weekly streak row, goal card progress lists, and options for logging progress, editing, or deleting goals
class GoalTrackingFragment : Fragment() {

    // View binding backing reference
    private var _binding: FragmentGoalTrackingBinding? = null

    // View binding property accessor
    private val binding get() = _binding!!

    // ViewModel instance for goal operations
    private val viewModel: GoalTrackingViewModel by viewModels()

    // RecyclerView adapter for goal items
    private lateinit var adapter: GoalTrackingAdapter

    // List of loaded tracked goals
    private var goals: List<TrackedGoalDto> = emptyList()

    // Inflates the fragment layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }
    // Sets up UI components, RecyclerView adapter, window insets, and observers
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        adapter = GoalTrackingAdapter(onClick = { goal -> showGoalMenu(goal) })
        binding.recyclerGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGoals.adapter = adapter

        renderStreak()
        observeState()
    }

    // Refreshes goals on fragment resume
    override fun onResume() {
        super.onResume()
        viewModel.loadGoals()
    }

    // ---------------------------------------------------------------- setup

    // Adjusts padding to respect system bars and cutouts
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollGoals.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    // Configures header buttons and navigation drawer action
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

    // Sets click listeners for bottom navigation bar
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            // navGoals: already here.
            navStats.setOnClickListener { findNavController().navigate(R.id.action_goals_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_goals_to_settings) }
            fabAdd.setOnClickListener { findNavController().navigate(R.id.action_goals_to_addGoal) }
        }
    }

    // ---------------------------------------------------------------- state
    // Observes goal list UI state from ViewModel
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is GoalTrackingUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.textEmptyOrError.isVisible = false
                        }
                        is GoalTrackingUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            goals = state.goals
                            render()
                        }
                        is GoalTrackingUiState.Error -> {
                            binding.progressLoading.isVisible = false
                            binding.textEmptyOrError.isVisible = true
                            binding.textEmptyOrError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // Renders loaded goal rows and updates streak bar
    private fun render() {
        val today = LocalDate.now()

        val rows = goals.map { goal ->
            GoalRow(goal, GoalProgress.periodValue(goal, today), GoalProgress.percent(goal, today))
        }
        adapter.submitList(rows)

        binding.textEmptyOrError.isVisible = rows.isEmpty()
        binding.textEmptyOrError.setText(R.string.goaltrack_empty)

        renderStreak()
    }

    // Renders seven daily indicators (Mon-Sun) and summary count for current streak
    private fun renderStreak() {
        val today = LocalDate.now()
        val days = GoalProgress.weekDays(goals, today)

        val container = binding.layoutStreak
        container.removeAllViews()

        for (day in days) {
            val item = ItemStreakDayBinding.inflate(layoutInflater, container, false)
            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

            item.textDayLabel.text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            item.textDayLabel.alpha = if (day.isToday) 1f else 0.7f

            when {
                day.state == DayState.MET -> {
                    item.imageDay.setBackgroundResource(R.drawable.bg_goaltrack_streak_met)
                    item.imageDay.setImageResource(R.drawable.ic_goaltrack_check)
                    item.root.contentDescription = getString(R.string.goaltrack_day_met, dayName)
                }
                day.isToday -> {
                    item.imageDay.setBackgroundResource(R.drawable.bg_goaltrack_streak_today)
                    item.imageDay.setImageDrawable(null)
                    item.root.contentDescription = getString(R.string.goaltrack_day_today, dayName)
                }
                else -> {
                    item.imageDay.setBackgroundResource(R.drawable.bg_goaltrack_streak_dot)
                    item.imageDay.setImageDrawable(null)
                    item.imageDay.alpha = if (day.state == DayState.FUTURE) 0.5f else 1f
                    item.root.contentDescription = getString(
                        if (day.state == DayState.FUTURE) R.string.goaltrack_day_upcoming else R.string.goaltrack_day_missed,
                        dayName
                    )
                }
            }
            container.addView(item.root)
        }

        val streak = GoalProgress.currentStreak(goals, today)
        binding.textStreakSummary.text = when {
            !GoalProgress.hasDailyGoals(goals) -> getString(R.string.goaltrack_streak_no_daily)
            streak == 0 -> getString(R.string.goaltrack_streak_none)
            else -> resources.getQuantityString(R.plurals.goaltrack_streak_days, streak, streak)
        }
    }

    // -------------------------------------------------------------- actions

    // Shows goal options menu (Log progress, Edit, Delete)
    private fun showGoalMenu(goal: TrackedGoalDto) {
        val items = arrayOf(
            getString(R.string.goaltrack_action_log),
            getString(R.string.goaltrack_action_edit),
            getString(R.string.goaltrack_action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(goal.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showLogProgressDialog(goal)
                    1 -> openEditor(goal)
                    2 -> confirmDelete(goal)
                }
            }
            .show()
    }

    // Navigates to edit screen with selected goal ID
    private fun openEditor(goal: TrackedGoalDto) {
        findNavController().navigate(
            R.id.action_goals_to_addGoal,
            bundleOf(AddGoalTrackingFragment.ARG_GOAL_ID to goal.goalId)
        )
    }

    // Shows dialog to input progress value for today
    private fun showLogProgressDialog(goal: TrackedGoalDto) {
        val today = LocalDate.now()
        val todayValue = GoalProgress.valueOn(goal, today)
        val padding = (24 * resources.displayMetrics.density).toInt()

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (todayValue > 0) {
                setText(
                    if (todayValue == todayValue.toLong().toDouble()) todayValue.toLong().toString()
                    else todayValue.toString()
                )
            }
            setSelectAllOnFocus(true)
        }
        val container = FrameLayout(requireContext()).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.goaltrack_log_title, goal.name))
            .setMessage(getString(R.string.goaltrack_log_message, goal.unit))
            .setView(container)
            .setPositiveButton(R.string.goaltrack_save) { _, _ ->
                val value = input.text.toString().trim().replace(',', '.').toDoubleOrNull()
                if (value == null || value < 0 || value > 1_000_000) {
                    Toast.makeText(requireContext(), R.string.goaltrack_error_progress, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.logProgress(goal.goalId, today.toString(), value) { success, errorMessage ->
                        if (!success && _binding != null) showToast(errorMessage)
                    }
                }
            }
            .setNegativeButton(R.string.goaltrack_cancel, null)
            .show()
    }

    // Prompts confirmation dialog to delete goal
    private fun confirmDelete(goal: TrackedGoalDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.goaltrack_delete_title)
            .setMessage(getString(R.string.goaltrack_delete_message, goal.name))
            .setPositiveButton(R.string.goaltrack_delete) { _, _ ->
                viewModel.deleteGoal(goal.goalId) { success, errorMessage ->
                    if (!success && _binding != null) showToast(errorMessage)
                }
            }
            .setNegativeButton(R.string.goaltrack_cancel, null)
            .show()
    }

    // Displays toast message with error text
    private fun showToast(message: String?) {
        Toast.makeText(
            requireContext(),
            message ?: getString(R.string.goaltrack_error_generic),
            Toast.LENGTH_LONG
        ).show()
    }

    // Cleans up view binding when fragment view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//---------------------------------------END OF FILE-------------------------------------------------