package com.example.kenkosahealthapp_prog7314.ui.medication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentMedicationBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate

// Fragment responsible for displaying the user's medications.
// It handles the medication lists, navigation, loading states and user interactions.
class MedicationFragment : Fragment() {

    // View binding reference.
    // It is nullable because the Fragment's view can be destroyed while the Fragment remains alive.
    private var _binding: FragmentMedicationBinding? = null

    // Non-null shortcut used between onCreateView() and onDestroyView().
    private val binding get() = _binding!!

    // Gets the MedicationViewModel associated with this Fragment.
    private val viewModel: MedicationViewModel by viewModels()

    // Adapter for normal medications.
    private lateinit var pillAdapter: MedicationAdapter

    // Adapter for vitamins.
    private lateinit var vitaminAdapter: MedicationAdapter

    // Stores the complete list of medications returned from the server.
    private var medications: List<MedicationDto> = emptyList()

    // false = only medications due today are displayed.
    // true = all medications added by the user are displayed.
    private var showAll = false

    // Creates the Fragment's view using the FragmentMedication XML layout.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after the Fragment's view has been created.
    // This is where the screen's components and listeners are set up.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up spacing around the system status/navigation bars.
        setupWindowInsets()

        // Set up the menu button.
        setupHeader()

        // Set up the bottom navigation buttons.
        setupBottomNav()

        // Create and connect the RecyclerView adapters.
        setupLists()


        // Toggle between today's medications and all medications.
        binding.textSeeAll.setOnClickListener {
            showAll = !showAll
            render()
        }

        observeState()
    }

    // Reload medications whenever the Fragment becomes visible again.
    override fun onResume() {
        super.onResume()
        viewModel.loadMedications()
    }

    // ---------------------------------------------------------------- setup

    // Handles edge-to-edge screen layout.
    // Adds padding so content does not overlap the phone's system bars.
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollMedication.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    // Sets up the buttons at the top of the screen.
    private fun setupHeader() {
        // The nav drawer lives in HomeFragment, so the menu button goes back to Home and asks it to open the drawer.
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Notification bell is intentionally non-functional for now (same as Home / Settings).
    }

    // Sets up the buttons at the bottom of the medication screen.
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_medication_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_medication_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_medication_to_settings) }
            fabAdd.setOnClickListener { findNavController().navigate(R.id.action_medication_to_addMedication) }
        }
    }

    // Creates the two RecyclerViews used on the screen.
    private fun setupLists() {
        pillAdapter = newAdapter()
        vitaminAdapter = newAdapter()

        binding.recyclerPills.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPills.adapter = pillAdapter
        binding.recyclerVitamins.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerVitamins.adapter = vitaminAdapter
    }

    // Creates a MedicationAdapter and connects its actions to the ViewModel/navigation.
    private fun newAdapter() = MedicationAdapter(
        onTakenToggle = { row ->
            viewModel.setTaken(row.medication, LocalDate.now().toString(), !row.takenToday)
        },
        onClick = { medication -> openEditor(medication) },
        onLongClick = { medication -> confirmDelete(medication) }
    )

    // ---------------------------------------------------------------- state

    // Observes the current state of the medication list from the ViewModel.
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {

            // Only collect the Flow while the Fragment is visible/started.
            // This prevents unnecessary work when the screen is not active.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MedicationUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.textError.isVisible = false
                            binding.layoutContent.isVisible = false
                        }
                        is MedicationUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = false
                            binding.layoutContent.isVisible = true
                            medications = state.medications
                            render()
                        }
                        // Something went wrong while loading medications.
                        is MedicationUiState.Error -> {
                            binding.progressLoading.isVisible = false
                            binding.layoutContent.isVisible = false
                            binding.textError.isVisible = true
                            binding.textError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // Rebuilds the content displayed on the screen using the current medication list.
    private fun render() {
        // Get today's date.
        val today = LocalDate.now()

        // If showAll is false, only medications due today are displayed.
        // If showAll is true, every medication is displayed.
        val visible = medications.filter { showAll || MedicationSchedule.isDueOn(it, today) }
        val pills = visible.filter { it.category != MedicationSchedule.CATEGORY_VITAMIN }
        val vitamins = visible.filter { it.category == MedicationSchedule.CATEGORY_VITAMIN }

        pillAdapter.submitList(pills.sortedBy { it.reminderTime }.map { toRow(it, today) })
        vitaminAdapter.submitList(vitamins.sortedBy { it.reminderTime }.map { toRow(it, today) })

        binding.textEmptyPills.isVisible = pills.isEmpty()
        binding.textEmptyVitamins.isVisible = vitamins.isEmpty()
        binding.textSeeAll.setText(if (showAll) R.string.medication_show_today else R.string.medication_see_all)

        renderHero(today)
    }

    // Converts a MedicationDto into a MedicationRow for the RecyclerView.
    private fun toRow(medication: MedicationDto, today: LocalDate) = MedicationRow(
        medication = medication,
        takenToday = MedicationSchedule.isTaken(medication, today),
        timeLabel = if (medication.remindersEnabled) {
            MedicationSchedule.formatTime(requireContext(), medication.reminderTime)
        } else {
            getString(R.string.medication_no_reminder)
        }
    )

    // Updates the encouragement card and the number shown on the notification badge.
    // Vitamins are excluded from these calculations.
    private fun renderHero(today: LocalDate) {
        val tracked = medications.filter { it.category != MedicationSchedule.CATEGORY_VITAMIN }
        val missed = MedicationSchedule.missedDoses(tracked, today)
        val remainingToday = tracked.count {
            MedicationSchedule.isDueOn(it, today) && !MedicationSchedule.isTaken(it, today)
        }

        when {
            tracked.isEmpty() -> {
                binding.textHeroTitle.setText(R.string.medication_hero_title_empty)
                binding.textHeroMessage.setText(R.string.medication_hero_msg_empty)
            }
            missed == 0 -> {
                binding.textHeroTitle.setText(R.string.medication_hero_title_good)
                binding.textHeroMessage.setText(R.string.medication_hero_msg_good)
            }
            else -> {
                binding.textHeroTitle.setText(R.string.medication_hero_title_missed)
                binding.textHeroMessage.text =
                    resources.getQuantityString(R.plurals.medication_hero_msg_missed, missed, missed)
            }
        }

        binding.textBellBadge.isVisible = remainingToday > 0
        binding.textBellBadge.text = remainingToday.toString()
    }

    // -------------------------------------------------------------- actions
    // Opens the AddMedicationFragment in edit mode for an existing medication.
    private fun openEditor(medication: MedicationDto) {
        findNavController().navigate(
            R.id.action_medication_to_addMedication,
            bundleOf(AddMedicationFragment.ARG_MEDICATION_ID to medication.medicationId)
        )
    }

    // Displays a confirmation dialog before deleting a medication.
    private fun confirmDelete(medication: MedicationDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.medication_delete_title)
            .setMessage(getString(R.string.medication_delete_message, medication.name))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteMedication(medication.medicationId) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // Called when the Fragment's view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//---------------------------------------END OF FILE-------------------------------------------------