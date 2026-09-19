package com.example.kenkosahealthapp_prog7314.ui.medication

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
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddMedicationBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Add screen, and (when opened with [ARG_MEDICATION_ID]) the details / edit screen for an existing
 * medication. In edit mode the fields start locked; "Edit Medication" unlocks them.
 */
class AddMedicationFragment : Fragment() {

    private var _binding: FragmentAddMedicationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationViewModel by viewModels()

    private var medicationId: String? = null
    private val isEditMode get() = medicationId != null

    /** The medication as loaded from the server (edit mode only). */
    private var original: MedicationDto? = null

    private var reminderHour = 8
    private var reminderMinute = 0
    private var isLocked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        medicationId = arguments?.getString(ARG_MEDICATION_ID)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        updateReminderLabel()
        binding.rowReminderTime.setOnClickListener { pickReminderTime() }

        binding.buttonEditMedication.setOnClickListener { setLocked(false) }
        binding.buttonSaveMedication.setOnClickListener { attemptSave() }
        binding.buttonCancelMedication.setOnClickListener { findNavController().navigateUp() }

        if (isEditMode) {
            binding.textTitle.setText(R.string.medication_details_title)
            binding.buttonEditMedication.isEnabled = false // until the medication has loaded
            setLocked(true)
            observeEditState()
            viewModel.loadMedication(medicationId!!)
        } else {
            binding.textTitle.setText(R.string.add_medication_title)
            setLocked(false)
        }
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollAddMedication.updatePadding(top = bars.top)
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
        // Notification bell is intentionally non-functional for now (same as Home / Settings).
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_addMedication_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addMedication_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addMedication_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    // ------------------------------------------------------------ edit mode

    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is MedicationEditState.Idle -> Unit
                        is MedicationEditState.Loading -> setLoading(true)
                        is MedicationEditState.Loaded -> {
                            setLoading(false)
                            hideError()
                            // Only fill the form once per view, so coming back from the background
                            // doesn't wipe edits the user hasn't saved yet.
                            if (original == null) populate(state.medication)
                        }
                        is MedicationEditState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun populate(medication: MedicationDto) {
        original = medication

        binding.editName.setText(medication.name)
        binding.editDosage.setText(medication.dosage)

        binding.chipGroupType.check(
            if (medication.category == MedicationSchedule.CATEGORY_VITAMIN) R.id.chipVitamin else R.id.chipMedication
        )
        binding.chipGroupFrequency.check(
            when (medication.frequency) {
                MedicationSchedule.FREQUENCY_WEEKLY -> R.id.chipWeekly
                MedicationSchedule.FREQUENCY_MONTHLY -> R.id.chipMonthly
                else -> R.id.chipDaily
            }
        )

        binding.switchReminders.isChecked = medication.remindersEnabled
        val (hour, minute) = MedicationSchedule.parseTime(medication.reminderTime)
        reminderHour = hour
        reminderMinute = minute
        updateReminderLabel()

        binding.buttonEditMedication.isEnabled = true
    }

    /** Locked = read-only details view; unlocked = editable form. */
    private fun setLocked(locked: Boolean) {
        isLocked = locked

        binding.editName.isEnabled = !locked
        binding.editDosage.isEnabled = !locked
        binding.switchReminders.isEnabled = !locked
        binding.rowReminderTime.isEnabled = !locked
        for (group in listOf(binding.chipGroupType, binding.chipGroupFrequency)) {
            for (i in 0 until group.childCount) group.getChildAt(i).isEnabled = !locked
        }

        binding.buttonEditMedication.isVisible = locked
        binding.buttonSaveMedication.isVisible = !locked
    }

    // ------------------------------------------------------------- reminder

    private fun pickReminderTime() {
        if (isLocked) return
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
        binding.textReminderTime.text =
            MedicationSchedule.formatTime(requireContext(), MedicationSchedule.toHHmm(reminderHour, reminderMinute))
    }

    // ----------------------------------------------------------------- save

    private fun attemptSave() {
        val name = binding.editName.text.toString().trim()
        val dosage = binding.editDosage.text.toString().trim()

        when {
            name.isEmpty() -> return showError(getString(R.string.medication_error_name_required))
            name.length > 60 -> return showError(getString(R.string.medication_error_name_length))
            dosage.isEmpty() -> return showError(getString(R.string.medication_error_dosage_required))
            dosage.length > 30 -> return showError(getString(R.string.medication_error_dosage_length))
        }

        val category = if (binding.chipVitamin.isChecked) {
            MedicationSchedule.CATEGORY_VITAMIN
        } else {
            MedicationSchedule.CATEGORY_MEDICATION
        }
        val frequency = when (binding.chipGroupFrequency.checkedChipId) {
            R.id.chipWeekly -> MedicationSchedule.FREQUENCY_WEEKLY
            R.id.chipMonthly -> MedicationSchedule.FREQUENCY_MONTHLY
            else -> MedicationSchedule.FREQUENCY_DAILY
        }
        val remindersEnabled = binding.switchReminders.isChecked
        val reminderTime = MedicationSchedule.toHHmm(reminderHour, reminderMinute)
        val today = LocalDate.now().toString()

        hideError()
        setLoading(true)

        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            if (_binding != null) {
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: getString(R.string.medication_error_generic))
                }
            }
        }

        val id = medicationId
        if (id == null) {
            viewModel.createMedication(
                category, name, dosage, frequency, remindersEnabled, reminderTime,
                startDate = today,
                onResult = onResult
            )
        } else {
            // Changing how often a dose repeats restarts its schedule from today, so past days
            // aren't suddenly judged against the new frequency and counted as missed.
            val newStartDate = if (frequency != original?.frequency) today else null
            viewModel.updateMedication(
                id, category, name, dosage, frequency, remindersEnabled, reminderTime,
                startDate = newStartDate,
                onResult = onResult
            )
        }
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
        binding.buttonSaveMedication.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        original = null // the next view gets filled from the ViewModel again
        _binding = null
    }

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
    }
}
