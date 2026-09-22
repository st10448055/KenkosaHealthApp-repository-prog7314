package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentWellnessBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WellnessFragment : Fragment() {

    private var _binding: FragmentWellnessBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels()

    private lateinit var moodAdapter: MoodEntryAdapter
    private lateinit var moodButtons: Map<Mood, ImageButton>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWellnessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupVideos()
        setupMoodFaces()
        setupMoodList()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMoods()
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollWellness.updatePadding(top = bars.top)
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
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_wellness_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_wellness_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_wellness_to_settings) }
            fabAdd.setOnClickListener { findNavController().navigate(R.id.action_wellness_to_logMood) }
        }
    }

    /** Horizontal row of YouTube videos. Tapping one opens it in the YouTube app (or the browser). */
    private fun setupVideos() {
        binding.recyclerVideos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerVideos.adapter = MeditationVideoAdapter(MeditationVideos.all) { video ->
            openVideo(video)
        }
    }

    private fun openVideo(video: MeditationVideo) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.meditation_open_error, Toast.LENGTH_LONG).show()
        }
    }

    /** The five faces under "Log your Mood Today": tap one to start a new entry with it preselected. */
    private fun setupMoodFaces() {
        moodButtons = mapOf(
            Mood.VERY_HAPPY to binding.moodVeryHappy,
            Mood.HAPPY to binding.moodHappy,
            Mood.NEUTRAL to binding.moodNeutral,
            Mood.SAD to binding.moodSad,
            Mood.VERY_SAD to binding.moodVerySad
        )
        for ((mood, button) in moodButtons) {
            button.setOnClickListener {
                findNavController().navigate(
                    R.id.action_wellness_to_logMood,
                    bundleOf(LogMoodFragment.ARG_PRESELECT_MOOD to mood.name)
                )
            }
        }
    }

    private fun setupMoodList() {
        moodAdapter = MoodEntryAdapter(
            onClick = { entry ->
                findNavController().navigate(
                    R.id.action_wellness_to_logMood,
                    bundleOf(LogMoodFragment.ARG_MOOD_ID to entry.moodId)
                )
            },
            onLongClick = { entry -> confirmDelete(entry) }
        )
        binding.recyclerMoods.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMoods.adapter = moodAdapter
    }

    // ---------------------------------------------------------------- state

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MoodUiState.Loading -> {
                            binding.progressMoods.isVisible = true
                            binding.textMoodError.isVisible = false
                        }
                        is MoodUiState.Success -> {
                            binding.progressMoods.isVisible = false
                            binding.textMoodError.isVisible = false
                            renderEntries(state.entries)
                        }
                        is MoodUiState.Error -> {
                            binding.progressMoods.isVisible = false
                            binding.textMoodError.isVisible = true
                            binding.textMoodError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun renderEntries(entries: List<MoodEntryDto>) {
        val newestFirst = entries.sortedByDescending { it.loggedAt }
        moodAdapter.submitList(newestFirst.take(MAX_RECENT))
        binding.textEmptyMoods.isVisible = newestFirst.isEmpty()

        // Ring the face for the most recent mood logged today, if there is one.
        val today = LocalDate.now()
        val todaysMood = newestFirst
            .firstOrNull { toLocalDate(it.loggedAt) == today }
            ?.let { Mood.fromKey(it.mood) }
        for ((mood, button) in moodButtons) {
            button.setBackgroundResource(if (mood == todaysMood) R.drawable.bg_mood_selected else 0)
        }
    }

    private fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun confirmDelete(entry: MoodEntryDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mood_delete_title)
            .setMessage(R.string.mood_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMood(entry.moodId) { success, errorMessage ->
                    if (!success && _binding != null) {
                        Toast.makeText(
                            requireContext(),
                            errorMessage ?: getString(R.string.mood_error_generic),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_RECENT = 10
    }
}
