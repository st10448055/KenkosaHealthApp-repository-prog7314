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

    // View Binding is nullable because the Fragment's view can be destroyed
    // while the Fragment itself still exists.
    private var _binding: FragmentWellnessBinding? = null

    // Allows the rest of the Fragment to use the binding safely while the view exists.
    private val binding get() = _binding!!

    // ViewModel is retained across configuration changes and manages the mood data/state.
    private val viewModel: MoodViewModel by viewModels()

    private lateinit var moodAdapter: MoodEntryAdapter
    private lateinit var moodButtons: Map<Mood, ImageButton>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the Wellness screen layout using View Binding.
        _binding = FragmentWellnessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the different sections of the Wellness screen.
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

        // Reload moods whenever the screen becomes active.
        // This ensures newly created or edited moods appear in the list.
        viewModel.loadMoods()
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        // Adjust the screen padding so content does not overlap
        // with system elements such as the status and navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            binding.scrollWellness.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)

            insets
        }
    }

    private fun setupHeader() {
        // Open the navigation drawer through the HomeFragment.
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()

            // Store a flag on the HomeFragment's back stack entry so it knows
            // that the drawer should be opened when we return to it.
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment)
                    .savedStateHandle
                    .set(HomeFragment.KEY_OPEN_DRAWER, true)
            }

            nav.popBackStack(R.id.homeFragment, false)
        }

        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

    private fun setupBottomNav() {
        // Connect each navigation button to its corresponding destination.
        with(binding.bottomNav) {
            navHome.setOnClickListener {
                findNavController().popBackStack(R.id.homeFragment, false)
            }

            navGoals.setOnClickListener {
                findNavController().navigate(R.id.action_wellness_to_goals)
            }

            navStats.setOnClickListener {
                findNavController().navigate(R.id.action_wellness_to_statistics)
            }

            navSettings.setOnClickListener {
                findNavController().navigate(R.id.action_wellness_to_settings)
            }

            // The floating add button opens the Log Mood screen.
            fabAdd.setOnClickListener {
                findNavController().navigate(R.id.action_wellness_to_logMood)
            }
        }
    }

    // Horizontal row of YouTube meditation videos.
    // Tapping a video opens it in the YouTube app or the browser.
    private fun setupVideos() {
        binding.recyclerVideos.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // Use the shared meditation video list and adapter to populate the RecyclerView.
        binding.recyclerVideos.adapter =
            MeditationVideoAdapter(MeditationVideos.all) { video ->
                openVideo(video)
            }
    }

    private fun openVideo(video: MeditationVideo) {
        try {
            // ACTION_VIEW lets Android choose an appropriate app to open the YouTube URL.
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(video.url)
                )
            )
        } catch (e: ActivityNotFoundException) {
            // Show an error if the device has no application capable of opening the link.
            Toast.makeText(
                requireContext(),
                R.string.meditation_open_error,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // The five mood faces under "Log your Mood Today".
    // Selecting one opens Log Mood with that mood already selected.
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
                // Pass the selected mood to the Log Mood screen as a navigation argument.
                findNavController().navigate(
                    R.id.action_wellness_to_logMood,
                    bundleOf(
                        LogMoodFragment.ARG_PRESELECT_MOOD to mood.name
                    )
                )
            }
        }
    }

    private fun setupMoodList() {
        // Configure the adapter's actions for tapping and long-pressing mood entries.
        moodAdapter = MoodEntryAdapter(
            onClick = { entry ->
                // Open the selected mood in edit mode by passing its unique ID.
                findNavController().navigate(
                    R.id.action_wellness_to_logMood,
                    bundleOf(
                        LogMoodFragment.ARG_MOOD_ID to entry.moodId
                    )
                )
            },
            onLongClick = { entry ->
                // Long-pressing an entry opens the delete confirmation dialog.
                confirmDelete(entry)
            }
        )

        binding.recyclerMoods.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerMoods.adapter = moodAdapter
    }

    // ---------------------------------------------------------------- state

    private fun observeState() {
        // Collect the ViewModel's StateFlow while the Fragment's view is active.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {

                        // Show a progress indicator while mood data is loading.
                        is MoodUiState.Loading -> {
                            binding.progressMoods.isVisible = true
                            binding.textMoodError.isVisible = false
                        }

                        // Display the loaded mood entries.
                        is MoodUiState.Success -> {
                            binding.progressMoods.isVisible = false
                            binding.textMoodError.isVisible = false
                            renderEntries(state.entries)
                        }

                        // Display the error message if loading failed.
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
        // Sort moods from newest to oldest so the most recent entry appears first.
        val newestFirst = entries.sortedByDescending { it.loggedAt }

        // Only display the configured number of recent moods.
        moodAdapter.submitList(
            newestFirst.take(MAX_RECENT)
        )

        // Show the empty-state message when there are no mood entries.
        binding.textEmptyMoods.isVisible = newestFirst.isEmpty()

        // Find the most recent mood that was logged today.
        val today = LocalDate.now()

        val todaysMood = newestFirst
            .firstOrNull { toLocalDate(it.loggedAt) == today }
            ?.let { Mood.fromKey(it.mood) }

        // Highlight the face that matches today's most recently logged mood.
        for ((mood, button) in moodButtons) {
            button.setBackgroundResource(
                if (mood == todaysMood) {
                    R.drawable.bg_mood_selected
                } else {
                    0
                }
            )
        }
    }

    // Convert the backend timestamp into a LocalDate using the device's time zone.
    // This allows the app to correctly determine whether a mood was logged today.
    private fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    private fun confirmDelete(entry: MoodEntryDto) {
        // Ask the user to confirm before permanently deleting the mood entry.
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mood_delete_title)
            .setMessage(R.string.mood_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->

                // Delete the selected mood through the ViewModel.
                viewModel.deleteMood(entry.moodId) { success, errorMessage ->

                    // Only show an error if the deletion failed and the Fragment
                    // still has an active view binding.
                    if (!success && _binding != null) {
                        Toast.makeText(
                            requireContext(),
                            errorMessage
                                ?: getString(R.string.mood_error_generic),
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

        // Clear the binding when the Fragment's view is destroyed.
        // This prevents the Fragment from holding onto the old view hierarchy.
        _binding = null
    }

    private companion object {
        // Maximum number of recent mood entries displayed on the Wellness screen.
        const val MAX_RECENT = 10
    }
}