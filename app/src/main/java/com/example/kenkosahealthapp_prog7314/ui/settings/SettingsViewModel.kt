package com.example.kenkosahealthapp_prog7314.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.model.UserSettings
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        load()
        loadProfile()
    }

    private fun load() {
        val uid = userId ?: return
        viewModelScope.launch {
            val existing = userRepository.getSettings(uid)
            if (existing != null) {
                _settings.value = existing
            } else {
                // No settings doc yet for this user create one with defaults
                // so userId is always populated correctly.
                val defaults = UserSettings(userId = uid)
                userRepository.createDefaultSettings(uid)
                _settings.value = defaults
            }
        }
    }

    private fun loadProfile() {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                _profile.value = userRepository.getUser(uid)
            } catch (e: Exception) {
                // Offline / no doc: the screen falls back to the FirebaseAuth email.
            }
        }
    }

    private fun save(updated: UserSettings) {
        val uid = userId ?: return
        val withUserId = updated.copy(userId = uid)
        _settings.value = withUserId
        viewModelScope.launch { userRepository.updateSettings(withUserId) }
    }

    fun updateProfile(name: String, surname: String) {
        val uid = userId ?: return
        val current = _profile.value ?: User(
            userId = uid,
            email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        )
        _profile.value = current.copy(name = name, surname = surname)
        viewModelScope.launch {
            try {
                userRepository.updateUserName(uid, name, surname)
            } catch (e: Exception) {
                // Local state already updated; Firestore retries queued writes when back online.
            }
        }
    }

    fun setUnitSystem(value: String) = save(_settings.value.copy(unitSystem = value))
    fun setLanguage(value: String) = save(_settings.value.copy(language = value))
    fun setAppearanceMode(value: String) = save(_settings.value.copy(appearanceMode = value))
    fun setHapticsEnabled(value: Boolean) = save(_settings.value.copy(hapticsEnabled = value))
    fun setNotificationsEnabled(value: Boolean) = save(_settings.value.copy(notificationsEnabled = value))
    fun setDailyStepGoal(value: Int) = save(_settings.value.copy(dailyStepGoal = value))
    fun setCalorieTarget(value: Int) = save(_settings.value.copy(calorieTarget = value))
    fun setSleepTargetMinutes(value: Int) = save(_settings.value.copy(sleepTargetMinutes = value))
}
