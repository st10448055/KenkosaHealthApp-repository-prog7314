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

class MedicationFragment : Fragment() {

    private var _binding: FragmentMedicationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationViewModel by viewModels()

    private lateinit var pillAdapter: MedicationAdapter
    private lateinit var vitaminAdapter: MedicationAdapter

    private var medications: List<MedicationDto> = emptyList()

    /** false = only what's due today, true = every medication the user has added. */
    private var showAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupLists()

        binding.textSeeAll.setOnClickListener {
            showAll = !showAll
            render()
        }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMedications()
    }

    // ---------------------------------------------------------------- setup

    /** Edge-to-edge (targetSdk 36): keep content clear of the system bars, same as Settings. */
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

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_medication_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_medication_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_medication_to_settings) }
            fabAdd.setOnClickListener { findNavController().navigate(R.id.action_medication_to_addMedication) }
        }
    }

    private fun setupLists() {
        pillAdapter = newAdapter()
        vitaminAdapter = newAdapter()

        binding.recyclerPills.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPills.adapter = pillAdapter
        binding.recyclerVitamins.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerVitamins.adapter = vitaminAdapter
    }

    private fun newAdapter() = MedicationAdapter(
        onTakenToggle = { row ->
            viewModel.setTaken(row.medication, LocalDate.now().toString(), !row.takenToday)
        },
        onClick = { medication -> openEditor(medication) },
        onLongClick = { medication -> confirmDelete(medication) }
    )

    // ---------------------------------------------------------------- state

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun render() {
        val today = LocalDate.now()

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

    private fun toRow(medication: MedicationDto, today: LocalDate) = MedicationRow(
        medication = medication,
        takenToday = MedicationSchedule.isTaken(medication, today),
        timeLabel = if (medication.remindersEnabled) {
            MedicationSchedule.formatTime(requireContext(), medication.reminderTime)
        } else {
            getString(R.string.medication_no_reminder)
        }
    )

    /** Encouragement card + bell badge, both driven by the user's actual medication (vitamins excluded). */
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

    private fun openEditor(medication: MedicationDto) {
        findNavController().navigate(
            R.id.action_medication_to_addMedication,
            bundleOf(AddMedicationFragment.ARG_MEDICATION_ID to medication.medicationId)
        )
    }

    private fun confirmDelete(medication: MedicationDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.medication_delete_title)
            .setMessage(getString(R.string.medication_delete_message, medication.name))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteMedication(medication.medicationId) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
