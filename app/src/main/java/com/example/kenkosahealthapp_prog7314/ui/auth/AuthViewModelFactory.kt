package com.example.kenkosahealthapp_prog7314.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kenkosahealthapp_prog7314.data.repository.AuthRepository
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository


// Factory for constructing [AuthViewModel] with its required repository dependencies,
// since the default ViewModelProvider can't build ViewModels that take constructor arguments.

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    companion object {
        // OAuth 2.0 web client ID used to request a Google ID token during Google Sign-In
        const val WEB_CLIENT_ID = "493411333577-pdhb0e58557kncjlulavlgqt10rpic8f.apps.googleusercontent.com"
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Use applicationContext to avoid leaking an Activity/Fragment context into the ViewModel
        val authRepository = AuthRepository(context.applicationContext, WEB_CLIENT_ID)
        val userRepository = UserRepository()
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(authRepository, userRepository) as T
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------