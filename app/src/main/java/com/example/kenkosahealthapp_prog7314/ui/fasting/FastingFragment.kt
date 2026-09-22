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


class FastingFragment : Fragment() {

    private var _binding: FragmentFastingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FastingViewModel by viewModels()

    private lateinit var historyAdapter: FastEntryAdapter

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
            binding.buttonPrimary.isEnabled = false

            val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
                if (_binding != null) {
                    binding.buttonPrimary.isEnabled = true
                    if (!success) showToast(errorMessage)
                }
            }
            if (active != null) viewModel.endFast(active, onResult) else viewModel.startNow(onResult)
        }

        // The pencils next to Start and End open the fast in edit mode.
        binding.buttonEditStart.setOnClickListener { activeFast?.let { openEditor(it, startEditing = true) } }
        binding.buttonEditEnd.setOnClickListener { activeFast?.let { openEditor(it, startEditing = true) } }
    }

    private fun showDailyTip() {
        val tips = resources.getStringArray(R.array.fasting_tips)
        if (tips.isNotEmpty()) {
            binding.textTip.text = tips[LocalDate.now().dayOfYear % tips.size]
        }
    }

    // ---------------------------------------------------------------- state

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

    /** Keeps the elapsed time and the ring moving while the screen is showing. */
    private fun startTicker() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    renderActiveFast()
                    delay(TICK_MILLIS)
                }
            }
        }
    }

    private fun render() {
        activeFast = fasts.filter { it.active }.maxByOrNull { it.startTime }

        val finished = fasts.filter { !it.active }.sortedByDescending { it.startTime }.take(MAX_HISTORY)
        historyAdapter.submitList(finished)
        binding.textEmptyHistory.isVisible = finished.isEmpty()

        renderActiveFast()
    }

    private fun renderActiveFast() {
        val b = _binding ?: return
        val context = requireContext()
        val fast = activeFast

        if (fast == null) {
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
        val planned = (fast.endTime - fast.startTime).coerceAtLeast(1L)
        val durationText = FastTimeFormat.duration(context, elapsed)

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

    private fun openEditor(fast: FastDto, startEditing: Boolean) {
        findNavController().navigate(
            R.id.action_fasting_to_addFast,
            bundleOf(
                AddFastFragment.ARG_FAST_ID to fast.fastId,
                AddFastFragment.ARG_START_EDITING to startEditing
            )
        )
    }

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
        const val TICK_MILLIS = 30_000L
        const val MAX_HISTORY = 10
    }
}
