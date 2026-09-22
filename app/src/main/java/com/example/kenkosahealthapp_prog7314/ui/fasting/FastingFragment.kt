package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentFastingBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

// Main fasting screen: shows the live progress ring for the active fast (if any),
// a Start/End button, and a history list of past fasts.
class FastingFragment : Fragment() {

    private var _binding: FragmentFastingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FastingViewModel by viewModels()

    private lateinit var historyAdapter: FastEntryAdapter

    // Cached copy of the latest data from the ViewModel, used to recompute
    // activeFast/history whenever it changes.
    private var fasts: List<FastDto> = emptyList()
    private var activeFast: FastDto? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFastingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupHistory()
        setupButtons()
        showDailyTip()

        observeState()
        startTicker()
    }

    // Refresh the fast list every time the screen becomes visible again (e.g.
    // after returning from the add/edit screen), so any changes are reflected.
    override fun onResume() {
        super.onResume()
        viewModel.loadFasts()
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollFasting.updatePadding(top = bars.top)
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
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_fasting_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_fasting_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_fasting_to_settings) }
            fabAdd.setOnClickListener { findNavController().navigate(R.id.action_fasting_to_addFast) }
        }
    }

    // Configures the RecyclerView that shows finished fasts.
    private fun setupHistory() {
        historyAdapter = FastEntryAdapter(
            onClick = { fast -> openEditor(fast, startEditing = false) },
            onLongClick = { fast -> confirmDelete(fast) }
        )
        binding.recyclerFasts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFasts.adapter = historyAdapter
    }

    private fun setupButtons() {
        // Start / End Fasting
        binding.buttonPrimary.setOnClickListener {
            val active = activeFast
            // Disable immediately to prevent double-taps while the request is in flight.
            binding.buttonPrimary.isEnabled = false

            val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
                if (_binding != null) {
                    binding.buttonPrimary.isEnabled = true
                    if (!success) showToast(errorMessage)
                }
            }
            // If a fast is already active, this button ends it; otherwise it starts a new one.
            if (active != null) viewModel.endFast(active, onResult) else viewModel.startNow(onResult)
        }

        // The pencils next to Start and End open the fast in edit mode.
        binding.buttonEditStart.setOnClickListener { activeFast?.let { openEditor(it, startEditing = true) } }
        binding.buttonEditEnd.setOnClickListener { activeFast?.let { openEditor(it, startEditing = true) } }
    }

    // Picks a rotating "tip of the day" based on the day of the year, so it's
    // stable throughout a given day but changes daily.
    private fun showDailyTip() {
        val tips = resources.getStringArray(R.array.fasting_tips)
        if (tips.isNotEmpty()) {
            binding.textTip.text = tips[LocalDate.now().dayOfYear % tips.size]
        }
    }

    // ---------------------------------------------------------------- state
    // Collects the ViewModel's fast-list state and updates loading/error/content UI.
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is FastingUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.textError.isVisible = false
                        }
                        is FastingUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = false
                            fasts = state.fasts
                            render()
                        }
                        is FastingUiState.Error -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = true
                            binding.textError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // Keeps the elapsed time and the ring moving while the screen is showing.
    private fun startTicker() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Loop re-renders the active fast's elapsed time/ring every TICK_MILLIS,
                // automatically pausing/resuming with the lifecycle (STARTED-scoped).
                while (isActive) {
                    renderActiveFast()
                    delay(TICK_MILLIS)
                }
            }
        }
    }

    // Recomputes activeFast/history from the latest `fasts` list and refreshes the UI.
    private fun render() {
        activeFast = fasts.filter { it.active }.maxByOrNull { it.startTime }

        // Show only the most recent finished fasts, newest first, capped at MAX_HISTORY.
        val finished = fasts.filter { !it.active }.sortedByDescending { it.startTime }.take(MAX_HISTORY)
        historyAdapter.submitList(finished)
        binding.textEmptyHistory.isVisible = finished.isEmpty()

        renderActiveFast()
    }

    // Updates the ring, timer text, and start/end labels for the currently active fast
    // (or shows the "not fasting" empty state if there isn't one).
    private fun renderActiveFast() {
        val b = _binding ?: return
        val context = requireContext()
        val fast = activeFast

        if (fast == null) {
            // Empty state: no fast currently running.
            b.ringView.progress = 0f
            b.textRingLabel.isVisible = false
            b.textRingValue.setText(R.string.fasting_not_fasting)
            b.textRingHint.setText(R.string.fasting_ready)
            b.textRingHint.isVisible = true
            b.buttonPrimary.setText(R.string.fasting_start_now)
            b.textStartValue.setText(R.string.fasting_none)
            b.textEndValue.setText(R.string.fasting_none)
            b.buttonEditStart.isVisible = false
            b.buttonEditEnd.isVisible = false
            b.ringView.contentDescription = getString(R.string.fasting_not_fasting)
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = now - fast.startTime
        // Avoid divide-by-zero if start/end happen to be equal.
        val planned = (fast.endTime - fast.startTime).coerceAtLeast(1L)
        val durationText = FastTimeFormat.duration(context, elapsed)

        // Ring fills based on elapsed/planned; can exceed 1.0 once past the goal,
        // but FastingRingView clamps progress internally.
        b.ringView.progress = elapsed.toFloat() / planned.toFloat()
        b.textRingLabel.isVisible = true
        b.textRingValue.text = durationText
        b.textRingHint.setText(R.string.fasting_goal_reached)
        b.textRingHint.isVisible = now >= fast.endTime
        b.buttonPrimary.setText(R.string.fasting_end)
        b.textStartValue.text = FastTimeFormat.dayAndTime(context, fast.startTime)
        b.textEndValue.text = FastTimeFormat.dayAndTime(context, fast.endTime)
        b.buttonEditStart.isVisible = true
        b.buttonEditEnd.isVisible = true
        b.ringView.contentDescription = getString(R.string.fasting_for) + " " + durationText
    }

    // -------------------------------------------------------------- actions
    // Navigates to AddFastFragment for the given fast, either read-only or
    // straight into edit mode depending on `startEditing`.
    private fun openEditor(fast: FastDto, startEditing: Boolean) {
        findNavController().navigate(
            R.id.action_fasting_to_addFast,
            bundleOf(
                AddFastFragment.ARG_FAST_ID to fast.fastId,
                AddFastFragment.ARG_START_EDITING to startEditing
            )
        )
    }

    // Shows a confirmation dialog before deleting a history entry.
    private fun confirmDelete(fast: FastDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.fasting_delete_title)
            .setMessage(R.string.fasting_delete_message)
            .setPositiveButton(R.string.fasting_delete) { _, _ ->
                viewModel.deleteFast(fast.fastId) { success, errorMessage ->
                    if (!success && _binding != null) showToast(errorMessage)
                }
            }
            .setNegativeButton(R.string.fasting_cancel, null)
            .show()
    }

    private fun showToast(message: String?) {
        Toast.makeText(
            requireContext(),
            message ?: getString(R.string.fasting_error_generic),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        // How often the active-fast timer/ring refreshes while the screen is visible.
        const val TICK_MILLIS = 30_000L

        // Cap on how many past fasts are shown in the history list.
        const val MAX_HISTORY = 10
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------