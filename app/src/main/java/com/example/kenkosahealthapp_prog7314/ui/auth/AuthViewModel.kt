package com.example.kenkosahealthapp_prog7314.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.repository.AuthRepository
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signUp(name: String, surname: String, email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val authResult = authRepository.signUpWithEmail(email, password)
            authResult.fold(
                onSuccess = { firebaseUser ->
                    val newUser = User(
                        userId = firebaseUser.uid,
                        email = email,
                        name = name,
                        surname = surname
                    )
                    userRepository.createUserDocument(newUser)
                    userRepository.createDefaultSettings(firebaseUser.uid)
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { e -> _uiState.value = AuthUiState.Error(e.message ?: "Sign up failed") }
            )
        }
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { e -> _uiState.value = AuthUiState.Error(e.message ?: "Sign in failed") }
            )
        }
    }

    /** Step 1 of Google sign-in: ask Credential Manager for a Google ID token (needs an Activity context). */
    suspend fun requestGoogleIdToken(activityContext: Context): Result<String> =
        authRepository.requestGoogleIdToken(activityContext)

    /** Step 2 of Google sign-in: turn that token into a Firebase session and create the user doc if needed. */
    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.firebaseAuthWithGoogle(idToken)
            result.fold(
                onSuccess = { firebaseUser ->
                    val existing = userRepository.getUser(firebaseUser.uid)
                    if (existing == null) {
                        userRepository.createUserDocument(
                            User(
                                userId = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                name = firebaseUser.displayName ?: ""
                            )
                        )
                        userRepository.createDefaultSettings(firebaseUser.uid)
                    }
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { e -> _uiState.value = AuthUiState.Error(e.message ?: "Google sign in failed") }
            )
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(authRepository.sendPasswordReset(email).isSuccess) }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}