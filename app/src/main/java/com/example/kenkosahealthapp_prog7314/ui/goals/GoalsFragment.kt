package com.example.kenkosahealthapp_prog7314.ui.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.GoalDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentGoalsBinding
import kotlinx.coroutines.launch

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels()
    private lateinit var adapter: GoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        adapter = GoalsAdapter(
            onGoalClick = { goal -> showUpdateProgressPrompt(goal) },
            onGoalLongClick = { goal -> confirmDelete(goal) }
        )
        binding.recyclerGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGoals.adapter = adapter

        binding.buttonAddGoal.setOnClickListener {
            findNavController().navigate(R.id.action_goals_to_addGoal)
        }

        renderWeeklyStreakPlaceholder()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadGoals()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is GoalsUiState.Loading -> {
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.layoutEmptyOrError.visibility = View.GONE
                            binding.recyclerGoals.visibility = View.GONE
                        }
                        is GoalsUiState.Success -> {
                            binding.progressLoading.visibility = View.GONE
                            if (state.goals.isEmpty()) {
                                binding.recyclerGoals.visibility = View.GONE
                                binding.layoutEmptyOrError.visibility = View.VISIBLE
                                binding.textEmptyOrError.text = "No goals yet. Tap + to add one."
                            } else {
                                binding.recyclerGoals.visibility = View.VISIBLE
                                binding.layoutEmptyOrError.visibility = View.GONE
                                adapter.submitList(state.goals)
                            }
                        }
                        is GoalsUiState.Error -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.recyclerGoals.visibility = View.GONE
                            binding.layoutEmptyOrError.visibility = View.VISIBLE
                            binding.textEmptyOrError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // Overall weekly-streak dots (from Health Diary / goal completion) aren't
    // backed by an endpoint in the spec yet — rendered as static placeholders
    // for now. Swap for real data once a "day fully met goals" source exists.
    private fun renderWeeklyStreakPlaceholder() {
        val container = binding.layoutWeeklyStreak
        container.removeAllViews()
        val filledDays = 5
        for (i in 0 until 7) {
            val dot = ImageView(requireContext())
            val size = (32 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = (8 * resources.displayMetrics.density).toInt()
            dot.layoutParams = params
            dot.setImageResource(
                if (i < filledDays) android.R.drawable.checkbox_on_background
                else android.R.drawable.checkbox_off_background
            )
            dot.setBackgroundResource(
                if (i < filledDays) R.drawable.bg_streak_dot_filled
                else R.drawable.bg_streak_dot_empty
            )
            container.addView(dot)
        }
    }

    private fun showUpdateProgressPrompt(goal: GoalDto) {
        val input = android.widget.EditText(requireContext())
        input.setText(goal.currentValue.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Update progress: ${goal.name}")
            .setMessage("Target: ${goal.targetValue} ${goal.unit}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value == null || value < 0) {
                    android.widget.Toast.makeText(requireContext(), "Enter a valid non-negative number", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateProgress(goal.goalId, value)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(goal: GoalDto) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete goal?")
            .setMessage("This will permanently remove \"${goal.name}\".")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGoal(goal.goalId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}