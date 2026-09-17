package com.example.kenkosahealthapp_prog7314.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        load()
    }

    private fun load() {
        val uid = userId ?: return
        viewModelScope.launch {
            userRepository.getSettings(uid)?.let { _settings.value = it }
        }
    }

    private fun save(updated: UserSettings) {
        _settings.value = updated
        viewModelScope.launch { userRepository.updateSettings(updated) }
    }

    fun setUnitSystem(value: String) = save(_settings.value.copy(unitSystem = value))
    fun setLanguage(value: String) = save(_settings.value.copy(language = value))
    fun setAppearanceMode(value: String) = save(_settings.value.copy(appearanceMode = value))
    fun setHapticsEnabled(value: Boolean) = save(_settings.value.copy(hapticsEnabled = value))
    fun setNotificationsEnabled(value: Boolean) = save(_settings.value.copy(notificationsEnabled = value))
}