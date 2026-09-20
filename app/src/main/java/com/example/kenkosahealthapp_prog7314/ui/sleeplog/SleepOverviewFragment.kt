package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.os.Bundle
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSleepOverviewBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The Sleep screen: a ring with the selected day's total sleep and score, the week's bars (tap one to look at that
 * day), and the most recent nights. Tap a night to edit it, long-press to delete it.
 */
class SleepOverviewFragment : Fragment() {

    private var _binding: FragmentSleepOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SleepLogViewModel by viewModels()

    private lateinit var adapter: SleepLogAdapter
    private var records: List<SleepLogDto> = emptyList()
    private var selectedDay: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupDayLetters()

        adapter = SleepLogAdapter(
            onClick = { record -> openEditor(record) },
            onLongClick = { record -> confirmDelete(record) }
        )
        binding.recyclerSleep.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSleep.adapter = adapter

        binding.weekBars.onBarSelected = { index ->
            selectedDay = SleepMath.weekStart(LocalDate.now()).plusDays(index.toLong())
            render()
        }
        binding.buttonAdd.setOnClickListener { openAdd() }

        observeState()
        render()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSleep()
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollSleep.updatePadding(top = bars.top)
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
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_sleep_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_sleep_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_sleep_to_settings) }
            fabAdd.setOnClickListener { openAdd() }
        }
    }

    /** M T W T F S S under the bars (same seven equal slots the bars use). */
    private fun setupDayLetters() {
        val container = binding.layoutDayLetters
        container.removeAllViews()
        val monday = SleepMath.weekStart(LocalDate.now())
        for (offset in 0..6) {
            val letter = monday.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            val view = TextView(requireContext()).apply {
                text = letter
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(requireContext().getColor(R.color.settings_text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            container.addView(view)
        }
    }

    // ---------------------------------------------------------------- state

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SleepLogUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.textError.isVisible = false
                        }
                        is SleepLogUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = false
                            records = state.records
                            render()
                        }
                        is SleepLogUiState.Error -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = true
                            binding.textError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun render() {
        val context = requireContext()
        val today = LocalDate.now()
        val monday = SleepMath.weekStart(today)
        val week = (0..6).map { monday.plusDays(it.toLong()) }
        if (selectedDay !in week) selectedDay = today

        // Weekly bars
        binding.weekBars.setData(week.map { SleepMath.minutesOn(records, it) }, week.indexOf(selectedDay))

        // Ring + score for the selected day
        val total = SleepMath.minutesOn(records, selectedDay)
        binding.ringSleep.progress = total.toFloat() / SleepMath.TARGET_MINUTES
        binding.textTotalSleep.text = SleepMath.formatShort(context, total)
        binding.textSelectedDay.text = if (selectedDay == today) {
            getString(R.string.sleeplog_today)
        } else {
            DateUtils.formatDateTime(
                context,
                selectedDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
            )
        }

        val main = SleepMath.mainRecordOn(records, selectedDay)
        if (main == null) {
            binding.textScore.text = getString(R.string.sleeplog_score_none)
            binding.textQualityPill.isVisible = false
        } else {
            val score = SleepMath.score(main)
            binding.textScore.text = getString(R.string.sleeplog_score, score)
            binding.textQualityPill.setText(SleepMath.qualityLabelRes(SleepMath.qualityOf(score)))
            binding.textQualityPill.isVisible = true
        }

        // Recent nights
        adapter.submitList(records.sortedByDescending { it.sleepEnd }.take(MAX_RECENT))
        binding.textEmpty.isVisible = records.isEmpty()
    }

    // -------------------------------------------------------------- actions

    private fun openAdd() {
        findNavController().navigate(R.id.action_sleep_to_addSleep)
    }

    private fun openEditor(record: SleepLogDto) {
        findNavController().navigate(
            R.id.action_sleep_to_addSleep,
            bundleOf(AddSleepLogFragment.ARG_SLEEP_ID to record.sleepId)
        )
    }

    private fun confirmDelete(record: SleepLogDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sleeplog_delete_title)
            .setMessage(R.string.sleeplog_delete_message)
            .setPositiveButton(R.string.sleeplog_delete) { _, _ ->
                viewModel.deleteSleep(record.sleepId) { success, errorMessage ->
                    if (!success && _binding != null) {
                        Toast.makeText(
                            requireContext(),
                            errorMessage ?: getString(R.string.sleeplog_error_generic),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.sleeplog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_RECENT = 14
    }
}
