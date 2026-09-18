package com.example.kenkosahealthapp_prog7314.ui.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddGoalBinding

class AddGoalFragment : Fragment() {

    private var _binding: FragmentAddGoalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        // Keep the two RadioGroups for category mutually exclusive across both rows,
        // and update the unit field's lock state whenever category changes.
        binding.radioGroupCategory.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) binding.radioGroupCategory2.clearCheck()
            applyUnitLockForCategory()
        }
        binding.radioGroupCategory2.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) binding.radioGroupCategory.clearCheck()
            applyUnitLockForCategory()
        }

        applyUnitLockForCategory() // set initial state (Steps is checked by default)

        binding.buttonCreateGoal.setOnClickListener { attemptCreateGoal() }
    }

    /**
     * Locks the unit field to "hours" for Sleep goals, since SleepFragment's
     * ring target assumes SLEEP-category targetValue is always expressed in
     * hours. For every other category, the unit stays free-text as before.
     */
    private fun applyUnitLockForCategory() {
        val isSleep = binding.radioSleep.isChecked

        if (isSleep) {
            binding.editUnit.setText("hours")
            binding.editUnit.isEnabled = false
        } else {
            binding.editUnit.isEnabled = true
            // Only clear it if it was previously locked to "hours" by us;
            // don't clobber a unit the user already typed for another category.
            if (binding.editUnit.text.toString() == "hours") {
                binding.editUnit.setText("")
            }
        }
    }

    private fun attemptCreateGoal() {
        val name = binding.editGoalName.text.toString().trim()
        val targetText = binding.editTargetValue.text.toString().trim()
        val unit = binding.editUnit.text.toString().trim()
        val category = selectedCategory()
        val frequency = selectedFrequency()
        val remindersEnabled = binding.switchReminders.isChecked

        if (name.isEmpty()) {
            showError("Please enter a goal name")
            return
        }
        if (name.length > 60) {
            showError("Goal name must be 60 characters or fewer")
            return
        }
        val target = targetText.toDoubleOrNull()
        if (target == null || target <= 0) {
            showError("Enter a target value greater than 0")
            return
        }
        if (target > 1_000_000) {
            showError("Target value is unrealistically large")
            return
        }
        if (unit.isEmpty()) {
            showError("Please enter a unit (e.g. steps, ml, hours)")
            return
        }
        if (category == "SLEEP" && target > 24) {
            showError("Sleep target can't exceed 24 hours")
            return
        }

        hideError()
        setLoading(true)

        viewModel.createGoal(
            category = category,
            name = name,
            targetValue = target,
            unit = unit,
            frequency = frequency,
            remindersEnabled = remindersEnabled,
            onResult = { success, errorMessage ->
                if (_binding == null) return@createGoal
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: "Something went wrong. Please try again.")
                }
            }
        )
    }

    private fun selectedCategory(): String = when {
        binding.radioWater.isChecked -> "WATER"
        binding.radioSleep.isChecked -> "SLEEP"
        binding.radioExercise.isChecked -> "EXERCISE"
        binding.radioCustom.isChecked -> "CUSTOM"
        else -> "STEPS"
    }

    private fun selectedFrequency(): String = when {
        binding.radioWeekly.isChecked -> "WEEKLY"
        binding.radioMonthly.isChecked -> "MONTHLY"
        else -> "DAILY"
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.textError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressCreating.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonCreateGoal.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}