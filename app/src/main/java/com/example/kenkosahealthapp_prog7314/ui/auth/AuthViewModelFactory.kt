package com.example.kenkosahealthapp_prog7314.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kenkosahealthapp_prog7314.data.repository.AuthRepository
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    companion object {
        const val WEB_CLIENT_ID = "493411333577-pdhb0e58557kncjlulavlgqt10rpic8f.apps.googleusercontent.com"
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepository = AuthRepository(context.applicationContext, WEB_CLIENT_ID)
        val userRepository = UserRepository()
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(authRepository, userRepository) as T
    }
}