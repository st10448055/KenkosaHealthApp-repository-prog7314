package com.example.kenkosahealthapp_prog7314.ui.settings

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import com.example.kenkosahealthapp_prog7314.databinding.DialogEditProfileBinding
import com.example.kenkosahealthapp_prog7314.databinding.DialogNumberInputBinding
import com.example.kenkosahealthapp_prog7314.databinding.DialogSleepTargetBinding
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSettingsBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

// Main settings screen — profile editing, units/language/dark-mode/notification toggles,
// body metrics shortcut, and numeric goal targets (steps, calories, sleep).
class SettingsFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentSettingsBinding? = null
    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModelFactory() }

    private val unitOptions = listOf("Metric", "Imperial", "UK")
    private val languages = listOf("English" to "en", "Afrikaans" to "af", "isiXhosa" to "xh")

    // Latest settings snapshot, used to pre-fill dialogs.
    private var currentSettings = UserSettings()

    // True while we push stored values into the views, so listeners don't write them straight back.
    private var isApplyingRemoteState = false

    // Handles the result of the notification permission prompt, reverting the switch
    // and showing an explanation if the user denies it
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setNotificationsEnabled(granted)
            if (!granted) {
                _binding?.let {
                    isApplyingRemoteState = true
                    it.rowPushNotifications.switchToggle.isChecked = false
                    isApplyingRemoteState = false
                }
                Toast.makeText(
                    requireContext(),
                    R.string.settings_notifications_blocked,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupLabels()
        setupHeader()
        setupBottomNav()
        setupPreferenceListeners()
        setupGoalListeners()

        binding.buttonEditProfile.setOnClickListener { showEditProfileDialog() }

        // Observe settings and profile in parallel, only while the view is at least STARTED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.settings.collect { applySettings(it) } }
                launch { viewModel.profile.collect { applyProfile(it) } }
            }
        }
    }

    // ---------------------------------------------------------------- setup

    // The app is edge-to-edge (targetSdk 36), so keep content clear of the system bars.
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollSettings.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupLabels() {
        binding.rowUnits.textLabel.setText(R.string.settings_units)
        binding.rowPushNotifications.textLabel.setText(R.string.settings_push_notifications)
        binding.rowDarkMode.textLabel.setText(R.string.settings_dark_mode)
        binding.rowLanguage.textLabel.setText(R.string.settings_language)
        binding.rowBodyMetrics.textLabel.setText(R.string.metrics_settings_row)
        binding.rowDailySteps.textLabel.setText(R.string.settings_daily_steps)
        binding.rowCalorieTarget.textLabel.setText(R.string.settings_calorie_target)
        binding.rowSleepTarget.textLabel.setText(R.string.settings_sleep_target)
    }

    private fun setupHeader() {
        // The nav drawer lives in HomeFragment, so the menu button goes back
        // to Home and asks it to open the drawer.
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            nav.previousBackStackEntry?.savedStateHandle?.set(HomeFragment.KEY_OPEN_DRAWER, true)
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Notification bell is intentionally non-functional for now (same as Home).
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_settings_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_settings_to_statistics) }
            // navSettings: already on this screen.
            // fabAdd: TODO wire up quick-add once its destination is decided.
        }
    }

    // Wires up units, language, notifications, and dark mode rows/toggles
    private fun setupPreferenceListeners() {
        binding.rowUnits.root.setOnClickListener {
            showChoiceDialog(
                R.string.settings_units,
                unitOptions,
                unitOptions.indexOf(currentSettings.unitSystem)
            ) { viewModel.setUnitSystem(unitOptions[it]) }
        }

        binding.rowLanguage.root.setOnClickListener {
            showChoiceDialog(
                R.string.settings_language,
                languages.map { it.first },
                languages.indexOfFirst { it.second == currentSettings.language }
            ) { index ->
                val code = languages[index].second
                viewModel.setLanguage(code)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
        }

        binding.rowPushNotifications.root.setOnClickListener {
            binding.rowPushNotifications.switchToggle.toggle()
        }
        binding.rowPushNotifications.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingRemoteState) return@setOnCheckedChangeListener
            if (isChecked && !hasNotificationPermission()) {
                // Saved once the user answers the system prompt.
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNotificationsEnabled(isChecked)
            }
        }

        binding.rowDarkMode.root.setOnClickListener {
            binding.rowDarkMode.switchToggle.toggle()
        }
        binding.rowDarkMode.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isApplyingRemoteState) return@setOnCheckedChangeListener
            val mode = if (isChecked) "Dark" else "Light"
            viewModel.setAppearanceMode(mode)
            // Applying the theme recreates the activity, so save first.
            ThemeManager.set(requireContext().applicationContext, mode)
        }
    }

    // Wires up the body metrics shortcut and the numeric goal target rows
    private fun setupGoalListeners() {
        // Body metrics (date of birth, gender, height, weight, activity level)
        binding.rowBodyMetrics.root.setOnClickListener {
            findNavController().navigate(R.id.action_global_metrics)
        }

        binding.rowDailySteps.root.setOnClickListener {
            showNumberDialog(
                title = R.string.settings_daily_steps,
                hint = R.string.settings_steps_hint,
                current = currentSettings.dailyStepGoal,
                min = 1_000,
                max = 100_000
            ) { viewModel.setDailyStepGoal(it) }
        }

        binding.rowCalorieTarget.root.setOnClickListener {
            showNumberDialog(
                title = R.string.settings_calorie_target,
                hint = R.string.settings_calories_hint,
                current = currentSettings.calorieTarget,
                min = 800,
                max = 10_000
            ) { viewModel.setCalorieTarget(it) }
        }

        binding.rowSleepTarget.root.setOnClickListener { showSleepTargetDialog() }
    }

    // -------------------------------------------------------------- render

    // Pushes a settings snapshot into all the rows, suppressing listener callbacks while doing so
    private fun applySettings(settings: UserSettings) {
        currentSettings = settings
        isApplyingRemoteState = true

        binding.rowUnits.textValue.text = settings.unitSystem
        binding.rowLanguage.textValue.text =
            languages.firstOrNull { it.second == settings.language }?.first ?: languages.first().first

        // Only show notifications as "on" if the system permission is actually granted.
        binding.rowPushNotifications.switchToggle.isChecked =
            settings.notificationsEnabled && hasNotificationPermission()
        binding.rowDarkMode.switchToggle.isChecked = isDarkModeOn(settings.appearanceMode)

        binding.rowDailySteps.textValue.text = String.format(Locale.getDefault(), "%,d", settings.dailyStepGoal)
        binding.rowCalorieTarget.textValue.text =
            String.format(Locale.getDefault(), "%,d kcal", settings.calorieTarget)
        binding.rowSleepTarget.textValue.text = formatDuration(settings.sleepTargetMinutes)

        isApplyingRemoteState = false
    }

    // Renders the profile header, falling back through Firestore name -> FirebaseAuth display name
    // -> email prefix -> a generic default, in case the Firestore profile doc isn't populated yet
    private fun applyProfile(user: User?) {
        val authUser = FirebaseAuth.getInstance().currentUser
        val fullName = listOfNotNull(user?.name, user?.surname)
            .joinToString(" ")
            .trim()
        val displayName = fullName.ifEmpty {
            authUser?.displayName?.takeIf { it.isNotBlank() }
                ?: authUser?.email?.substringBefore('@')
                ?: getString(R.string.settings_default_name)
        }
        val email = user?.email?.takeIf { it.isNotBlank() } ?: authUser?.email.orEmpty()

        binding.textProfileName.text = displayName
        binding.textProfileEmail.text = email
        binding.textAvatar.text = initialsOf(displayName)
    }

    // Builds initials for the avatar circle: first+last initial for multi-word names,
    // first two letters for a single word, "?" if there's nothing to work with
    private fun initialsOf(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    private fun formatDuration(totalMinutes: Int): String =
        String.format(Locale.getDefault(), "%dh %02dm", totalMinutes / 60, totalMinutes % 60)

    // Light is the default: only an explicit "Dark" turns the switch on.
    private fun isDarkModeOn(appearanceMode: String): Boolean = appearanceMode == "Dark"

    private fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    // -------------------------------------------------------------- dialogs

    // Generic single-choice picker dialog (used for units and language)
    private fun showChoiceDialog(
        @StringRes title: Int,
        options: List<String>,
        checkedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray<CharSequence>(), checkedIndex) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Generic numeric input dialog with range validation (used for steps and calorie target).
    // Overrides the positive button's default dismiss behavior so an invalid value keeps the dialog open.
    private fun showNumberDialog(
        @StringRes title: Int,
        @StringRes hint: Int,
        current: Int,
        min: Int,
        max: Int,
        onSave: (Int) -> Unit
    ) {
        val dialogBinding = DialogNumberInputBinding.inflate(layoutInflater)
        dialogBinding.inputLayout.hint = getString(hint)
        dialogBinding.editNumber.setText(current.toString())
        dialogBinding.editNumber.setSelection(dialogBinding.editNumber.length())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null) // overridden below so invalid input keeps the dialog open
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val value = dialogBinding.editNumber.text?.toString()?.trim()?.toIntOrNull()
                if (value == null || value !in min..max) {
                    dialogBinding.inputLayout.error = getString(R.string.settings_error_range, min, max)
                } else {
                    onSave(value)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    // Sleep target dialog using two number pickers: hours (4-12) and minutes in 15-min steps
    private fun showSleepTargetDialog() {
        val dialogBinding = DialogSleepTargetBinding.inflate(layoutInflater)
        val minuteSteps = arrayOf("00", "15", "30", "45")

        dialogBinding.pickerHours.apply {
            minValue = 4
            maxValue = 12
            value = (currentSettings.sleepTargetMinutes / 60).coerceIn(4, 12)
        }
        dialogBinding.pickerMinutes.apply {
            minValue = 0
            maxValue = minuteSteps.size - 1
            displayedValues = minuteSteps
            value = ((currentSettings.sleepTargetMinutes % 60) / 15).coerceIn(0, minuteSteps.size - 1)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_sleep_target)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val minutes = dialogBinding.pickerHours.value * 60 + dialogBinding.pickerMinutes.value * 15
                viewModel.setSleepTargetMinutes(minutes)
            }
            .show()
    }

    // Edit profile dialog for first name / surname, with basic required-field validation
    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val profile = viewModel.profile.value
        dialogBinding.editFirstName.setText(profile?.name.orEmpty())
        dialogBinding.editSurname.setText(profile?.surname.orEmpty())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.editFirstName.text?.toString()?.trim().orEmpty()
                val surname = dialogBinding.editSurname.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    dialogBinding.layoutFirstName.error = getString(R.string.settings_error_name_required)
                } else {
                    viewModel.updateProfile(name, surname)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks by releasing the binding reference
        _binding = null
    }
}