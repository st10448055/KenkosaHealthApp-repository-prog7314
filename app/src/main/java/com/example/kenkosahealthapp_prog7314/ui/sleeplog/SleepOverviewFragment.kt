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

// Fragment responsible for displaying the main Sleep screen.
// It shows the selected day's sleep, weekly sleep bars, sleep score,
// and the user's most recent sleep records.
class SleepOverviewFragment : Fragment() {

    // Binding is nullable because the Fragment's view can be destroyed
    // before the Fragment itself is destroyed.
    private var _binding: FragmentSleepOverviewBinding? = null

    // The non-null binding can safely be used between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Gets the ViewModel associated with this Fragment.
    // The ViewModel survives configuration changes such as screen rotation.
    private val viewModel: SleepLogViewModel by viewModels()

    // Adapter used to display the recent sleep records in the RecyclerView.
    private lateinit var adapter: SleepLogAdapter

    // Stores the sleep records received from the ViewModel.
    private var records: List<SleepLogDto> = emptyList()

    // Keeps track of which day is currently selected on the weekly chart.
    private var selectedDay: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflates the Fragment layout using View Binding.
        _binding = FragmentSleepOverviewBinding.inflate(
            inflater,
            container,
            false
        )

        // Returns the root view so Android can display the Fragment.
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sets up the different parts of the screen.
        setupWindowInsets()
        setupHeader()
        setupBottomNav()
        setupDayLetters()

        // Creates the adapter and defines what happens when a record
        // is clicked or long-pressed.
        adapter = SleepLogAdapter(
            onClick = { record -> openEditor(record) },
            onLongClick = { record -> confirmDelete(record) }
        )

        // The sleep records are displayed vertically in the RecyclerView.
        binding.recyclerSleep.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerSleep.adapter = adapter

        // When the user selects one of the weekly bars, calculate which
        // date that bar represents and update the selected day.
        binding.weekBars.onBarSelected = { index ->
            selectedDay =
                SleepMath.weekStart(LocalDate.now())
                    .plusDays(index.toLong())

            // Re-render the screen using the newly selected day.
            render()
        }

        // The Add button opens the Add Sleep screen.
        binding.buttonAdd.setOnClickListener {
            openAdd()
        }

        // Starts observing the ViewModel's UI state.
        observeState()

        // Displays the initial screen using the current data.
        render()
    }

    override fun onResume() {
        super.onResume()

        // Reload the sleep records whenever the screen becomes active.
        // This ensures changes made on the Add/Edit screen are shown.
        viewModel.loadSleep()
    }

    // ------------------------------------------------ setup

    private fun setupWindowInsets() {

        // Adjusts the layout so content does not overlap with system UI,
        // such as the status bar or device display cutout.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            // Adds padding at the top for the status bar.
            binding.scrollSleep.updatePadding(top = bars.top)

            // Adds padding at the bottom for the navigation/system bar.
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)

            // Returns the insets so they can continue being handled.
            insets
        }
    }

    private fun setupHeader() {

        // Opens the navigation drawer through the Home Fragment.
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()

            // Stores a value in the Home Fragment's saved state.
            // The Home screen can use this to know that the drawer should open.
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment)
                    .savedStateHandle
                    .set(HomeFragment.KEY_OPEN_DRAWER, true)
            }

            // Navigates back to the Home Fragment.
            nav.popBackStack(R.id.homeFragment, false)
        }

        // The notification bell is currently not connected to any functionality.
    }

    private fun setupBottomNav() {

        // Sets up navigation for each item in the bottom navigation bar.
        with(binding.bottomNav) {

            // Returns to the Home screen.
            navHome.setOnClickListener {
                findNavController()
                    .popBackStack(R.id.homeFragment, false)
            }

            // Opens the Goals screen.
            navGoals.setOnClickListener {
                findNavController()
                    .navigate(R.id.action_sleep_to_goals)
            }

            // Opens the Statistics screen.
            navStats.setOnClickListener {
                findNavController()
                    .navigate(R.id.action_sleep_to_statistics)
            }

            // Opens the Settings screen.
            navSettings.setOnClickListener {
                findNavController()
                    .navigate(R.id.action_sleep_to_settings)
            }

            // The floating add button also opens the Add Sleep screen.
            fabAdd.setOnClickListener {
                openAdd()
            }
        }
    }

    // Creates the seven day letters displayed underneath the weekly bars.
    // The letters are generated dynamically instead of being hard-coded.
    private fun setupDayLetters() {
        val container = binding.layoutDayLetters

        // Clears any existing views before adding the day labels.
        container.removeAllViews()

        // Gets the Monday of the current week.
        val monday = SleepMath.weekStart(LocalDate.now())

        // Creates one label for each day from Monday to Sunday.
        for (offset in 0..6) {

            // Gets the short day name using the user's device locale.
            val letter =
                monday
                    .plusDays(offset.toLong())
                    .dayOfWeek
                    .getDisplayName(
                        TextStyle.NARROW,
                        Locale.getDefault()
                    )

            // Creates a TextView for the day letter.
            val view = TextView(requireContext()).apply {
                text = letter
                gravity = Gravity.CENTER
                textSize = 11f

                // Uses the app's secondary text colour.
                setTextColor(
                    requireContext()
                        .getColor(R.color.settings_text_secondary)
                )

                // Each day gets an equal amount of horizontal space.
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Adds the newly created TextView to the container.
            container.addView(view)
        }
    }

    // ------------------------------------------------ state

    private fun observeState() {

        // Starts a coroutine that is tied to the Fragment's view lifecycle.
        viewLifecycleOwner.lifecycleScope.launch {

            // Only collects the StateFlow while the Fragment is visible/started.
            // This prevents unnecessary work when the screen is not active.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Collects changes from the ViewModel.
                viewModel.uiState.collect { state ->

                    // Reacts differently depending on the current UI state.
                    when (state) {

                        // Shows the loading indicator while data is being retrieved.
                        is SleepLogUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.textError.isVisible = false
                        }

                        // Displays the successfully loaded sleep records.
                        is SleepLogUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            binding.textError.isVisible = false

                            // Stores the records locally so render() can use them.
                            records = state.records

                            // Updates the screen with the new data.
                            render()
                        }

                        // Displays an error message if loading failed.
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

    // Updates all of the displayed sleep information using the current records.
    private fun render() {
        val context = requireContext()
        val today = LocalDate.now()

        // Gets the Monday of the current week.
        val monday = SleepMath.weekStart(today)

        // Creates a list containing all seven days of the current week.
        val week = (0..6).map {
            monday.plusDays(it.toLong())
        }

        // If the selected day is no longer in the current week,
        // reset the selection to today.
        if (selectedDay !in week) {
            selectedDay = today
        }

        // ------------------------------------------------ Weekly bars

        // Calculates the total sleep minutes for each day.
        // The list is passed to the weekly bar component.
        binding.weekBars.setData(
            week.map { SleepMath.minutesOn(records, it) },
            week.indexOf(selectedDay)
        )

        // ------------------------------------------------ Ring + score

        // Gets the total number of minutes slept on the selected day.
        val total = SleepMath.minutesOn(records, selectedDay)

        // Converts the total sleep into progress towards the 8-hour target.
        binding.ringSleep.progress =
            total.toFloat() / SleepMath.TARGET_MINUTES

        // Displays the total sleep using the shorter duration format.
        binding.textTotalSleep.text =
            SleepMath.formatShort(context, total)

        // Displays "Today" when today's date is selected.
        // Otherwise, it displays the selected date in a readable format.
        binding.textSelectedDay.text = if (selectedDay == today) {
            getString(R.string.sleeplog_today)
        } else {
            DateUtils.formatDateTime(
                context,
                selectedDay
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                DateUtils.FORMAT_SHOW_WEEKDAY or
                        DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_ABBREV_ALL
            )
        }

        // Finds the longest sleep record that ended on the selected day.
        // This record is used when calculating the sleep quality score.
        val main = SleepMath.mainRecordOn(records, selectedDay)

        if (main == null) {

            // If there is no sleep record for the selected day,
            // show that there is no score available.
            binding.textScore.text =
                getString(R.string.sleeplog_score_none)

            binding.textQualityPill.isVisible = false

        } else {

            // Calculates the sleep score using the selected day's main record.
            val score = SleepMath.score(main)

            // Displays the numerical sleep score.
            binding.textScore.text =
                getString(R.string.sleeplog_score, score)

            // Converts the score into a quality category and displays
            // the corresponding text in the quality pill.
            binding.textQualityPill.setText(
                SleepMath.qualityLabelRes(
                    SleepMath.qualityOf(score)
                )
            )

            binding.textQualityPill.isVisible = true
        }

        // ------------------------------------------------ Recent nights

        // Sorts the sleep records from newest to oldest.
        // Only the most recent 14 records are displayed in the list.
        adapter.submitList(
            records
                .sortedByDescending { it.sleepEnd }
                .take(MAX_RECENT)
        )

        // Shows the empty message when there are no sleep records.
        binding.textEmpty.isVisible = records.isEmpty()
    }

    // ------------------------------------------------ actions

    // Opens the screen used to add a new sleep record.
    private fun openAdd() {
        findNavController()
            .navigate(R.id.action_sleep_to_addSleep)
    }

    // Opens the Add Sleep screen in edit mode.
    // The selected record's ID is passed through the navigation Bundle.
    private fun openEditor(record: SleepLogDto) {
        findNavController().navigate(
            R.id.action_sleep_to_addSleep,
            bundleOf(
                AddSleepLogFragment.ARG_SLEEP_ID to record.sleepId
            )
        )
    }

    // Shows a confirmation dialog before deleting a sleep record.
    private fun confirmDelete(record: SleepLogDto) {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sleeplog_delete_title)
            .setMessage(R.string.sleeplog_delete_message)

            // Only deletes the record when the user confirms.
            .setPositiveButton(R.string.sleeplog_delete) { _, _ ->

                // Sends the record ID to the ViewModel for deletion.
                viewModel.deleteSleep(record.sleepId) { success, errorMessage ->

                    // If the deletion failed, display the error to the user.
                    if (!success && _binding != null) {
                        Toast.makeText(
                            requireContext(),
                            errorMessage
                                ?: getString(
                                    R.string.sleeplog_error_generic
                                ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // Closing the dialog without confirming does not delete anything.
            .setNegativeButton(R.string.sleeplog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clears the binding when the Fragment's view is destroyed.
        // This helps prevent memory leaks because the Fragment can outlive its view.
        _binding = null
    }

    // Maximum number of recent sleep records shown in the list.
    private companion object {
        const val MAX_RECENT = 14
    }
}