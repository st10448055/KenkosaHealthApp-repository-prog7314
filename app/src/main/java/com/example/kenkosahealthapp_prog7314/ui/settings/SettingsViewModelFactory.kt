package com.example.kenkosahealthapp_prog7314.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository

// Factory for constructing SettingsViewModel with its UserRepository dependency,
// since the default ViewModelProvider can't build ViewModels that take constructor arguments.
class SettingsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(UserRepository()) as T
    }
}