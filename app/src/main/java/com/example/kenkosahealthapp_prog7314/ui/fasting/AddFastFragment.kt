package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
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
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentAddFastBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

// Screen used for both "add a new fast" and "view/edit an existing fast".
// Which mode it's in depends on whether a fastId argument was passed in.
class AddFastFragment : Fragment() {

    // Nullable backing field + non-null accessor is the standard view-binding
    // pattern so the binding can be released in onDestroyView without leaking the view.
    private var _binding: FragmentAddFastBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FastingViewModel by viewModels()

    // Null when creating a brand-new fast; set to the existing fast's ID when editing.
    private var fastId: String? = null
    private val isEditMode get() = fastId != null

    // The fast as loaded from the server (edit mode only).
    private var original: FastDto? = null

    // Local working copies of the start/end times the user is choosing via the
    // date/time pickers. Seconds are zeroed out since the UI only shows H:M.
    private val startTime: Calendar = Calendar.getInstance().apply { clearSeconds() }
    private val endTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, FastingViewModel.DEFAULT_GOAL_HOURS.toInt())
        clearSeconds()
    }

    // Locked means read-only "details" view (edit mode, before tapping Edit).
    private var isLocked = false

    // Whether the screen should open already unlocked/editable (used when
    // coming from the pencil icons on the main fasting screen).
    private var startEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pull nav arguments: which fast (if any) we're editing, and whether to
        // start in edit mode straight away.
        fastId = arguments?.getString(ARG_FAST_ID)
        startEditing = arguments?.getBoolean(ARG_START_EDITING, false) == true

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        // Wire up the date/time chips to open the relevant picker dialog.
        binding.chipStartDate.setOnClickListener { pickDate(startTime, limitToToday = true) }
        binding.chipStartTime.setOnClickListener { pickTime(startTime) }
        binding.chipEndDate.setOnClickListener { pickDate(endTime, limitToToday = false) }
        binding.chipEndTime.setOnClickListener { pickTime(endTime) }

        binding.buttonEditFast.setOnClickListener { setLocked(false) }
        binding.buttonSaveFast.setOnClickListener { attemptSave() }
        binding.buttonCancelFast.setOnClickListener { findNavController().navigateUp() }
        binding.buttonDeleteFast.setOnClickListener { confirmDelete() }

        // Show the default (new-fast) start/end values immediately, before any
        // server data has loaded.
        updateLabels()

        if (isEditMode) {
            binding.textTitle.setText(R.string.fasting_details_title)
            binding.buttonEditFast.isEnabled = false // until the fast has loaded
            binding.buttonDeleteFast.isEnabled = false
            binding.buttonDeleteFast.isVisible = true
            // Start locked/read-only; populate() will unlock it later if startEditing is true.
            setLocked(true)
            observeEditState()
            viewModel.loadFast(fastId!!)
        } else {
            binding.textTitle.setText(R.string.fasting_add_title)
            setLocked(false)
        }
    }

    // ---------------------------------------------------------------- setup

    // Applies system bar/cutout insets as padding so content isn't drawn under
    // the status bar or the bottom nav isn't drawn under gesture/nav bars.
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollAddFast.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupHeader() {
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            // Tell the home screen to open its nav drawer once we pop back to it.
            // Wrapped in runCatching in case the home fragment isn't on the back stack.
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

    // Bottom nav bar destinations; this screen doesn't have its own fasting tab
    // action since we're already within the fasting flow.
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    // ------------------------------------------------------------ edit mode

    // Observes the ViewModel's edit-state flow (only relevant when editing an
    // existing fast) and updates the UI/loading/error state accordingly.
    private fun observeEditState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editState.collect { state ->
                    when (state) {
                        is FastEditState.Idle -> Unit
                        is FastEditState.Loading -> setLoading(true)
                        is FastEditState.Loaded -> {
                            setLoading(false)
                            hideError()
                            // Only fill the form once per view, so coming back from the background
                            // doesn't wipe edits the user hasn't saved yet.
                            if (original == null) populate(state.fast)
                        }
                        is FastEditState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    // Fills the form with the server-loaded fast data on first successful load.
    private fun populate(fast: FastDto) {
        original = fast
        startTime.timeInMillis = fast.startTime
        endTime.timeInMillis = fast.endTime
        updateLabels()

        binding.buttonEditFast.isEnabled = true
        binding.buttonDeleteFast.isEnabled = true
        if (startEditing) setLocked(false)
    }

    // Locked = read-only details view; unlocked = editable form.
    private fun setLocked(locked: Boolean) {
        isLocked = locked

        // Disable the date/time chips while locked so taps don't open the pickers.
        binding.chipStartDate.isEnabled = !locked
        binding.chipStartTime.isEnabled = !locked
        binding.chipEndDate.isEnabled = !locked
        binding.chipEndTime.isEnabled = !locked

        // Toggle between "Edit" button (locked) and "Save" button (unlocked).
        binding.buttonEditFast.isVisible = locked
        binding.buttonSaveFast.isVisible = !locked
    }

    // ------------------------------------------------------------ date/time

    // Refreshes all the chip labels and the computed duration text from the
    // current startTime/endTime Calendar values.
    private fun updateLabels() {
        val context = requireContext()
        val dateFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH

        binding.chipStartDate.text = DateUtils.formatDateTime(context, startTime.timeInMillis, dateFlags)
        binding.chipStartTime.text = DateUtils.formatDateTime(context, startTime.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
        binding.chipEndDate.text = DateUtils.formatDateTime(context, endTime.timeInMillis, dateFlags)
        binding.chipEndTime.text = DateUtils.formatDateTime(context, endTime.timeInMillis, DateUtils.FORMAT_SHOW_TIME)

        // Only show the duration summary once end is after start (avoids a
        // confusing negative/zero duration while the user is mid-edit).
        val length = endTime.timeInMillis - startTime.timeInMillis
        binding.textDuration.isVisible = length > 0
        if (length > 0) {
            binding.textDuration.text = getString(R.string.fasting_duration, FastTimeFormat.duration(context, length))
        }
    }

    // Opens a DatePickerDialog for the given Calendar target (start or end).
    private fun pickDate(target: Calendar, limitToToday: Boolean) {
        if (isLocked) return
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                target.set(year, month, day)
                updateLabels()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (limitToToday) datePicker.maxDate = System.currentTimeMillis() // a fast can't have started in the future
        }.show()
    }

    // Opens a TimePickerDialog for the given Calendar target (start or end).
    private fun pickTime(target: Calendar) {
        if (isLocked) return
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                target.set(Calendar.HOUR_OF_DAY, hour)
                target.set(Calendar.MINUTE, minute)
                target.clearSeconds()
                updateLabels()
            },
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    // ----------------------------------------------------------------- save
    // Validates the chosen start/end times, then creates or updates the fast
    // depending on whether we're in edit mode.

    private fun attemptSave() {
        val start = startTime.timeInMillis
        val end = endTime.timeInMillis
        val now = System.currentTimeMillis()

        // One minute of slacks so "now" never trips the check.
        when {
            end <= start -> return showError(getString(R.string.fasting_error_end_before_start))
            end - start > MAX_FAST_MILLIS -> return showError(getString(R.string.fasting_error_too_long))
            start > now + 60_000L -> return showError(getString(R.string.fasting_error_start_future))
            original?.active == false && end > now + 60_000L ->
                return showError(getString(R.string.fasting_error_finished_future))
        }

        hideError()
        setLoading(true)

        val onResult: (Boolean, String?) -> Unit = { success, errorMessage ->
            // Guard against the view having been destroyed while the request was in flight.
            if (_binding != null) {
                setLoading(false)
                if (success) {
                    findNavController().navigateUp()
                } else {
                    showError(errorMessage ?: getString(R.string.fasting_error_generic))
                }
            }
        }

        val id = fastId
        if (id == null) {
            viewModel.createFast(start, end, onResult)
        } else {
            viewModel.updateFast(id, start, end, onResult)
        }
    }

    // Shows a confirmation dialog before deleting the current fast.
    private fun confirmDelete() {
        val id = fastId ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.fasting_delete_title)
            .setMessage(R.string.fasting_delete_message)
            .setPositiveButton(R.string.fasting_delete) { _, _ ->
                setLoading(true)
                viewModel.deleteFast(id) { success, errorMessage ->
                    if (_binding == null) return@deleteFast
                    setLoading(false)
                    if (success) {
                        findNavController().navigateUp()
                    } else {
                        showError(errorMessage ?: getString(R.string.fasting_error_generic))
                    }
                }
            }
            .setNegativeButton(R.string.fasting_cancel, null)
            .show()
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
        binding.buttonSaveFast.isEnabled = !loading
    }

    // Small extension to zero out seconds/millis so times displayed only show H:M.
    private fun Calendar.clearSeconds() {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        original = null // the next view gets filled from the ViewModel again
        _binding = null
    }

    companion object {
        // Bundle argument keys used when navigating to this fragment.
        const val ARG_FAST_ID = "fastId"
        const val ARG_START_EDITING = "startEditing"
        // A fast can't be logged as longer than 7 days.
        private const val MAX_FAST_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------