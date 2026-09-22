package com.example.kenkosahealthapp_prog7314.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kenkosahealthapp_prog7314.data.model.User
import com.example.kenkosahealthapp_prog7314.data.repository.AuthRepository
import com.example.kenkosahealthapp_prog7314.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


 //Represents the current state of an authentication operation (sign up, login, Google sign-in),
 //observed by the UI to show loading indicators, navigate on success, or display errors.

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}


 // ViewModel shared by the login, sign-up, and Google sign-in flows.
 // Delegates actual auth calls to [AuthRepository] and user profile creation to [UserRepository],
 // exposing a single [AuthUiState] stream for the UI to observe.
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

     // Private mutable state, only this ViewModel can update it
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

     // Public read-only state exposed to the UI
     val uiState: StateFlow<AuthUiState> = _uiState

    // ------------------------------------------------------------------------------------------------------------------------

      // Creates a new email/password account, then creates the corresponding
      // Firestore user document and default settings for that user.
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

    // ------------------------------------------------------------------------------------------------------------------------
      // Signs an existing user in with email and password.
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
    // ------------------------------------------------------------------------------------------------------------------------

      // Signs the user in via Google using the ID token obtained from the Google Sign-In flow.
      // If this is the user's first time signing in with Google, a new Firestore profile
      // and default settings are created for them.

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

    // ------------------------------------------------------------------------------------------------------------------------
     // Retrieves the Intent needed to launch the Google Sign-In flow.
     fun getGoogleSignInIntent() = authRepository.getGoogleSignInIntent()

     //-----------------------------------------------------------------------------------------------------------------------
     //  Resets the UI state back to Idle, e.g. after handling a Success/Error state
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------