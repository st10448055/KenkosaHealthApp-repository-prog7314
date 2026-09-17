package com.example.kenkosahealthapp_prog7314.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModelFactory() }

    private val languages = listOf("English" to "en", "Afrikaans" to "af", "isiXhosa" to "xh")

    private var isApplyingRemoteState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spinnerLanguage.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            languages.map { it.first }
        )

        wireListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { applyToViews(it) }
            }
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_settings_to_start)
        }
    }

    private fun applyToViews(settings: UserSettings) {
        isApplyingRemoteState = true

        when (settings.unitSystem) {
            "Imperial" -> binding.radioImperial.isChecked = true
            "UK" -> binding.radioUk.isChecked = true
            else -> binding.radioMetric.isChecked = true
        }

        when (settings.appearanceMode) {
            "Light" -> binding.radioLight.isChecked = true
            "Dark" -> binding.radioDark.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }

        val languageIndex = languages.indexOfFirst { it.second == settings.language }
        if (languageIndex >= 0) binding.spinnerLanguage.setSelection(languageIndex)

        binding.switchHaptics.isChecked = settings.hapticsEnabled
        binding.switchNotifications.isChecked = settings.notificationsEnabled
        binding.textSubscriptionStatus.text = settings.subscriptionStatus

        isApplyingRemoteState = false
    }

    private fun wireListeners() {
        binding.radioGroupUnits.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingRemoteState) return@setOnCheckedChangeListener
            val value = when (checkedId) {
                R.id.radioImperial -> "Imperial"
                R.id.radioUk -> "UK"
                else -> "Metric"
            }
            viewModel.setUnitSystem(value)
        }

        binding.radioGroupAppearance.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingRemoteState) return@setOnCheckedChangeListener
            val value = when (checkedId) {
                R.id.radioLight -> "Light"
                R.id.radioDark -> "Dark"
                else -> "System"
            }
            viewModel.setAppearanceMode(value)
        }

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (!isApplyingRemoteState) viewModel.setLanguage(languages[position].second)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            if (!isApplyingRemoteState) viewModel.setHapticsEnabled(isChecked)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (!isApplyingRemoteState) viewModel.setNotificationsEnabled(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}