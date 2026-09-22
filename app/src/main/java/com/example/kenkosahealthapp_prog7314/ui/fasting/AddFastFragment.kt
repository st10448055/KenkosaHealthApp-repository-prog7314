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


class AddFastFragment : Fragment() {

    private var _binding: FragmentAddFastBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FastingViewModel by viewModels()

    private var fastId: String? = null
    private val isEditMode get() = fastId != null

    /** The fast as loaded from the server (edit mode only). */
    private var original: FastDto? = null

    private val startTime: Calendar = Calendar.getInstance().apply { clearSeconds() }
    private val endTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, FastingViewModel.DEFAULT_GOAL_HOURS.toInt())
        clearSeconds()
    }

    private var isLocked = false
    private var startEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fastId = arguments?.getString(ARG_FAST_ID)
        startEditing = arguments?.getBoolean(ARG_START_EDITING, false) == true

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        binding.chipStartDate.setOnClickListener { pickDate(startTime, limitToToday = true) }
        binding.chipStartTime.setOnClickListener { pickTime(startTime) }
        binding.chipEndDate.setOnClickListener { pickDate(endTime, limitToToday = false) }
        binding.chipEndTime.setOnClickListener { pickTime(endTime) }

        binding.buttonEditFast.setOnClickListener { setLocked(false) }
        binding.buttonSaveFast.setOnClickListener { attemptSave() }
        binding.buttonCancelFast.setOnClickListener { findNavController().navigateUp() }
        binding.buttonDeleteFast.setOnClickListener { confirmDelete() }

        updateLabels()

        if (isEditMode) {
            binding.textTitle.setText(R.string.fasting_details_title)
            binding.buttonEditFast.isEnabled = false // until the fast has loaded
            binding.buttonDeleteFast.isEnabled = false
            binding.buttonDeleteFast.isVisible = true
            setLocked(true)
            observeEditState()
            viewModel.loadFast(fastId!!)
        } else {
            binding.textTitle.setText(R.string.fasting_add_title)
            setLocked(false)
        }
    }

    // ---------------------------------------------------------------- setup

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
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_addFast_to_settings) }
            // fabAdd: already on the add screen.
        }
    }

    // ------------------------------------------------------------ edit mode

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

    private fun populate(fast: FastDto) {
        original = fast
        startTime.timeInMillis = fast.startTime
        endTime.timeInMillis = fast.endTime
        updateLabels()

        binding.buttonEditFast.isEnabled = true
        binding.buttonDeleteFast.isEnabled = true
        if (startEditing) setLocked(false)
    }

    /** Locked = read-only details view; unlocked = editable form. */
    private fun setLocked(locked: Boolean) {
        isLocked = locked

        binding.chipStartDate.isEnabled = !locked
        binding.chipStartTime.isEnabled = !locked
        binding.chipEndDate.isEnabled = !locked
        binding.chipEndTime.isEnabled = !locked

        binding.buttonEditFast.isVisible = locked
        binding.buttonSaveFast.isVisible = !locked
    }

    // ------------------------------------------------------------ date/time

    private fun updateLabels() {
        val context = requireContext()
        val dateFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH

        binding.chipStartDate.text = DateUtils.formatDateTime(context, startTime.timeInMillis, dateFlags)
        binding.chipStartTime.text = DateUtils.formatDateTime(context, startTime.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
        binding.chipEndDate.text = DateUtils.formatDateTime(context, endTime.timeInMillis, dateFlags)
        binding.chipEndTime.text = DateUtils.formatDateTime(context, endTime.timeInMillis, DateUtils.FORMAT_SHOW_TIME)

        val length = endTime.timeInMillis - startTime.timeInMillis
        binding.textDuration.isVisible = length > 0
        if (length > 0) {
            binding.textDuration.text = getString(R.string.fasting_duration, FastTimeFormat.duration(context, length))
        }
    }

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

    private fun attemptSave() {
        val start = startTime.timeInMillis
        val end = endTime.timeInMillis
        val now = System.currentTimeMillis()

        // One minute of slack so "now" never trips the check.
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
        const val ARG_FAST_ID = "fastId"
        const val ARG_START_EDITING = "startEditing"
        private const val MAX_FAST_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
