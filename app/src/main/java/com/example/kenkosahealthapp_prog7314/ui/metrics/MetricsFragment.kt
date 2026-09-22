package com.example.kenkosahealthapp_prog7314.ui.metrics

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.kenkosahealthapp_prog7314.data.model.ActivityLevel
import com.example.kenkosahealthapp_prog7314.data.model.Gender
import com.example.kenkosahealthapp_prog7314.data.model.HeightUnit
import com.example.kenkosahealthapp_prog7314.data.model.MetricsLimits
import com.example.kenkosahealthapp_prog7314.data.model.UnitConversions
import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.example.kenkosahealthapp_prog7314.data.model.WeightUnit
import com.example.kenkosahealthapp_prog7314.databinding.FragmentMetricsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Asks for date of birth, gender, height, weight and activity level.
 *
 * - Opened with [ARG_ONBOARDING] = true (Home does this for a user with no metrics): first-login prompt, with a
 *   "Skip for now" button and the system back button disabled.
 * - Opened without it (from Settings): same form, pre-filled, with Cancel.
 *
 * Height can be entered in cm or ft/in and weight in kg or lb. Switching unit converts what's already typed.
 * Which unit is shown by default always follows the Settings unit-of-measurement picker
 * (Metric / Imperial / UK); saving here also writes the resulting unit system back to Settings,
 * so the two screens stay in sync in both directions.
 */
class MetricsFragment : Fragment() {

    private var _binding: FragmentMetricsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MetricsViewModel by viewModels()

    private var isOnboarding = false
    private var dateOfBirth: LocalDate? = null
    private var heightUnit = HeightUnit.CM
    private var weightUnit = WeightUnit.KG

    /** Raw Settings value ("Metric" / "Imperial" / "UK"), kept so saving can preserve "UK" rather
     *  than collapsing it into "Imperial" when the chips already match what Settings had. */
    private var currentUnitSystem = "Metric"

    /** True while we set chips from code, so their listeners don't react to it. */
    private var isApplyingState = false

    /** The form is filled from saved data once per view; false again after a rotation with saved state restored. */
    private var formFilled = false

    private val genderChips by lazy {
        mapOf(
            R.id.chipFemale to Gender.FEMALE,
            R.id.chipMale to Gender.MALE,
            R.id.chipNonBinary to Gender.NON_BINARY,
            R.id.chipPreferNot to Gender.PREFER_NOT_TO_SAY
        )
    }

    private val activityRadios by lazy {
        mapOf(
            R.id.radioSedentary to ActivityLevel.SEDENTARY,
            R.id.radioLight to ActivityLevel.LIGHT,
            R.id.radioModerate to ActivityLevel.MODERATE,
            R.id.radioVeryActive to ActivityLevel.VERY_ACTIVE,
            R.id.radioExtraActive to ActivityLevel.EXTRA_ACTIVE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isOnboarding = arguments?.getBoolean(ARG_ONBOARDING, false) == true

        setupWindowInsets()
        setupModeTexts()
        setupDateOfBirth()
        setupUnitChips()

        binding.buttonSave.setOnClickListener { attemptSave() }
        binding.buttonSecondary.setOnClickListener { onSecondaryClicked() }

        if (isOnboarding) {
            // They have to answer or explicitly skip; back must not silently dismiss the prompt.
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() = Unit
                }
            )
        }

        if (savedInstanceState != null) {
            // Rotation etc.: the fields restore themselves; just bring back what they don't know about.
            dateOfBirth = savedInstanceState.getLong(STATE_DOB_EPOCH_DAY, Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE }?.let { LocalDate.ofEpochDay(it) }
            heightUnit = HeightUnit.valueOf(savedInstanceState.getString(STATE_HEIGHT_UNIT, HeightUnit.CM.name))
            weightUnit = WeightUnit.valueOf(savedInstanceState.getString(STATE_WEIGHT_UNIT, WeightUnit.KG.name))
            currentUnitSystem = savedInstanceState.getString(STATE_UNIT_SYSTEM, "Metric")
            applyHeightUnitUi()
            applyWeightUnitUi()
            renderDateOfBirth()
            formFilled = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formData.collect { data ->
                    if (data != null && !formFilled) {
                        formFilled = true
                        fillForm(data)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_HEIGHT_UNIT, heightUnit.name)
        outState.putString(STATE_WEIGHT_UNIT, weightUnit.name)
        outState.putString(STATE_UNIT_SYSTEM, currentUnitSystem)
        dateOfBirth?.let { outState.putLong(STATE_DOB_EPOCH_DAY, it.toEpochDay()) }
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollMetrics.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun setupModeTexts() {
        if (isOnboarding) {
            binding.textTitle.setText(R.string.metrics_title_onboarding)
            binding.textSubtitle.isVisible = true
            binding.buttonSecondary.setText(R.string.metrics_skip)
        } else {
            binding.textTitle.setText(R.string.metrics_title_edit)
            binding.textSubtitle.isVisible = false
            binding.buttonSecondary.setText(R.string.metrics_cancel)
        }
    }

    private fun setupDateOfBirth() {
        binding.fieldDob.setOnClickListener { pickDateOfBirth() }
    }

    private fun setupUnitChips() {
        binding.chipGroupHeightUnit.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isApplyingState) return@setOnCheckedStateChangeListener
            val newUnit = if (checkedIds.firstOrNull() == R.id.chipHeightFtIn) HeightUnit.FT_IN else HeightUnit.CM
            if (newUnit == heightUnit) return@setOnCheckedStateChangeListener

            val cm = readHeightCm() // read using the unit we're leaving
            heightUnit = newUnit
            applyHeightUnitUi()
            if (cm != null) showHeight(cm) else clearHeight()
        }

        binding.chipGroupWeightUnit.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isApplyingState) return@setOnCheckedStateChangeListener
            val newUnit = if (checkedIds.firstOrNull() == R.id.chipWeightLb) WeightUnit.LB else WeightUnit.KG
            if (newUnit == weightUnit) return@setOnCheckedStateChangeListener

            val kg = readWeightKg()
            weightUnit = newUnit
            applyWeightUnitUi()
            if (kg != null) showWeight(kg) else binding.editWeight.setText("")
        }

        applyHeightUnitUi()
        applyWeightUnitUi()
    }

    // ----------------------------------------------------------------- fill

    private fun fillForm(data: MetricsFormData) {
        val saved = data.existing

        // The unit shown always follows Settings, not whatever was saved with a previous
        // metrics entry - that's what keeps the Settings and Metrics screens in sync.
        heightUnit = data.defaultHeightUnit
        weightUnit = data.defaultWeightUnit
        currentUnitSystem = data.unitSystem
        applyHeightUnitUi()
        applyWeightUnitUi()

        if (saved == null) return

        dateOfBirth = saved.dateOfBirth
        renderDateOfBirth()

        genderChips.entries.firstOrNull { it.value == saved.gender }?.let { binding.chipGroupGender.check(it.key) }
        activityRadios.entries.firstOrNull { it.value == saved.activityLevel }
            ?.let { binding.radioGroupActivity.check(it.key) }

        showHeight(saved.heightCm)
        showWeight(saved.weightKg)
    }

    // ---------------------------------------------------------- date of birth

    private fun pickDateOfBirth() {
        val initial = dateOfBirth ?: LocalDate.now().minusYears(25)
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                dateOfBirth = LocalDate.of(year, month + 1, day)
                renderDateOfBirth()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // can't be born in the future
        }.show()
    }

    private fun renderDateOfBirth() {
        val dob = dateOfBirth
        if (dob == null) {
            binding.fieldDob.text = null
            binding.textAge.isVisible = false
            return
        }
        binding.fieldDob.text = dob.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        binding.textAge.text = getString(R.string.metrics_age, Period.between(dob, LocalDate.now()).years)
        binding.textAge.isVisible = true
    }

    // ------------------------------------------------------- height / weight

    private fun applyHeightUnitUi() {
        isApplyingState = true
        binding.chipGroupHeightUnit.check(
            if (heightUnit == HeightUnit.CM) R.id.chipHeightCm else R.id.chipHeightFtIn
        )
        isApplyingState = false

        val feetAndInches = heightUnit == HeightUnit.FT_IN
        binding.layoutHeightSecondary.isVisible = feetAndInches
        binding.textHeightPrimarySuffix.setText(if (feetAndInches) R.string.unit_ft else R.string.unit_cm)
        binding.editHeightPrimary.setHint(if (feetAndInches) R.string.metrics_hint_ft else R.string.metrics_hint_cm)
        binding.editHeightPrimary.inputType = if (feetAndInches) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    private fun applyWeightUnitUi() {
        isApplyingState = true
        binding.chipGroupWeightUnit.check(
            if (weightUnit == WeightUnit.KG) R.id.chipWeightKg else R.id.chipWeightLb
        )
        isApplyingState = false

        binding.textWeightSuffix.setText(if (weightUnit == WeightUnit.KG) R.string.unit_kg else R.string.unit_lb)
        binding.editWeight.setHint(if (weightUnit == WeightUnit.KG) R.string.metrics_hint_kg else R.string.metrics_hint_lb)
    }

    /** Puts a height that's stored in cm into the fields for whichever unit is showing. */
    private fun showHeight(cm: Double) {
        if (heightUnit == HeightUnit.CM) {
            binding.editHeightPrimary.setText(formatNumber(cm))
            binding.editHeightSecondary.setText("")
        } else {
            val (feet, inches) = UnitConversions.cmToFtIn(cm)
            binding.editHeightPrimary.setText(feet.toString())
            binding.editHeightSecondary.setText(formatNumber(inches))
        }
    }

    private fun clearHeight() {
        binding.editHeightPrimary.setText("")
        binding.editHeightSecondary.setText("")
    }

    private fun showWeight(kg: Double) {
        val value = if (weightUnit == WeightUnit.KG) kg else UnitConversions.kgToLb(kg)
        binding.editWeight.setText(formatNumber(value))
    }

    /** Height in cm from whatever is typed, or null if it's blank / not a number. */
    private fun readHeightCm(): Double? = when (heightUnit) {
        HeightUnit.CM -> parseNumber(binding.editHeightPrimary)
        HeightUnit.FT_IN -> {
            val feet = binding.editHeightPrimary.text.toString().trim().toIntOrNull()
            if (feet == null) null else UnitConversions.ftInToCm(feet, parseNumber(binding.editHeightSecondary) ?: 0.0)
        }
    }

    private fun readWeightKg(): Double? {
        val value = parseNumber(binding.editWeight) ?: return null
        return if (weightUnit == WeightUnit.KG) value else UnitConversions.lbToKg(value)
    }

    private fun parseNumber(field: EditText): Double? =
        field.text.toString().trim().replace(',', '.').toDoubleOrNull()

    /** 70.0 -> "70", 70.5 -> "70.5". */
    private fun formatNumber(value: Double): String =
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')

    /**
     * What to write back to Settings' unitSystem field. Metric/Imperial are decided by the
     * chips; "UK" is preserved only when it was already the active Settings value and the user
     * hasn't switched to metric - there's no way to tell "UK" and "Imperial" apart from the
     * chips alone, since both use ft/in and lb.
     */
    private fun resolveUnitSystemForSave(): String {
        val isImperial = heightUnit == HeightUnit.FT_IN || weightUnit == WeightUnit.LB
        return when {
            !isImperial -> "Metric"
            currentUnitSystem == "UK" -> "UK"
            else -> "Imperial"
        }
    }

    // ----------------------------------------------------------------- save

    private fun attemptSave() {
        val dob = dateOfBirth
        if (dob == null) return showError(R.string.metrics_error_dob)
        val age = Period.between(dob, LocalDate.now()).years
        if (dob.isAfter(LocalDate.now()) || age > MetricsLimits.MAX_AGE_YEARS) {
            return showError(R.string.metrics_error_dob_invalid)
        }

        val gender = genderChips[binding.chipGroupGender.checkedChipId]
            ?: return showError(R.string.metrics_error_gender)

        val heightCm = readHeightCm()
        if (heightCm == null || heightCm !in MetricsLimits.MIN_HEIGHT_CM..MetricsLimits.MAX_HEIGHT_CM) {
            return showError(R.string.metrics_error_height)
        }

        val weightKg = readWeightKg()
        if (weightKg == null || weightKg !in MetricsLimits.MIN_WEIGHT_KG..MetricsLimits.MAX_WEIGHT_KG) {
            return showError(R.string.metrics_error_weight)
        }

        val activity = activityRadios[binding.radioGroupActivity.checkedRadioButtonId]
            ?: return showError(R.string.metrics_error_activity)

        val metrics = UserMetrics(
            dateOfBirth = dob,
            gender = gender,
            heightCm = round2(heightCm),
            weightKg = round2(weightKg),
            activityLevel = activity,
            heightUnit = heightUnit,
            weightUnit = weightUnit
        )

        hideError()
        setLoading(true)
        viewModel.save(metrics, resolveUnitSystemForSave()) { success, errorMessage ->
            if (_binding == null) return@save
            setLoading(false)
            if (success) {
                Toast.makeText(requireContext(), R.string.metrics_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                showError(errorMessage ?: getString(R.string.metrics_error_generic))
            }
        }
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private fun onSecondaryClicked() {
        if (isOnboarding) {
            MetricsPromptState.skippedForUid = FirebaseAuth.getInstance().currentUser?.uid
        }
        findNavController().navigateUp()
    }

    private fun showError(messageRes: Int) {
        showError(getString(messageRes))
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
        binding.buttonSave.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        formFilled = false
        _binding = null
    }

    companion object {
        const val ARG_ONBOARDING = "onboarding"
        private const val STATE_HEIGHT_UNIT = "state_height_unit"
        private const val STATE_WEIGHT_UNIT = "state_weight_unit"
        private const val STATE_UNIT_SYSTEM = "state_unit_system"
        private const val STATE_DOB_EPOCH_DAY = "state_dob_epoch_day"
    }
}