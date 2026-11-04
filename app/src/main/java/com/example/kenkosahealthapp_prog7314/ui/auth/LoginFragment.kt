package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        arguments?.getString("email")?.takeIf { it.isNotEmpty() }?.let {
            binding.editEmail.setText(it)
        }

        binding.buttonSignIn.setOnClickListener { attemptLogin() }

        binding.buttonSignInWithGoogle.setOnClickListener { signInWithGoogle() }

        binding.textForgotPassword.setOnClickListener { sendPasswordReset() }

        binding.textGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_sign_up)
        }

        // Replaced by real biometric login in Part 3 (Step 14)
        binding.textUseFingerprint.setOnClickListener {
            showError(getString(R.string.auth_fingerprint_not_ready))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> setLoading(true)
                        is AuthUiState.Success -> {
                            setLoading(false)
                            findNavController().navigate(R.id.action_login_to_home)
                        }
                        is AuthUiState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                        is AuthUiState.Idle -> setLoading(false)
                    }
                }
            }
        }
    }

    /** Google SSO through Credential Manager: pick an account, then exchange the token with Firebase. */
    private fun signInWithGoogle() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            viewModel.requestGoogleIdToken(requireActivity()).fold(
                onSuccess = { idToken -> viewModel.signInWithGoogle(idToken) },
                onFailure = { e ->
                    setLoading(false)
                    when (e) {
                        // The user closed the sheet: not an error worth shouting about
                        is GetCredentialCancellationException -> Log.d(TAG, "Google sign-in cancelled")
                        is NoCredentialException -> showError(getString(R.string.auth_google_no_account))
                        else -> showError(getString(R.string.auth_google_failed))
                    }
                }
            )
        }
    }

    private fun sendPasswordReset() {
        val email = binding.editEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.auth_forgot_enter_email))
            return
        }
        binding.textError.visibility = View.GONE
        viewModel.sendPasswordReset(email) {
            // Same message either way, so we never reveal which emails have accounts
            if (_binding != null) {
                Toast.makeText(requireContext(), R.string.auth_forgot_sent, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.auth_enter_email_password))
            return
        }
        binding.textError.visibility = View.GONE
        viewModel.login(email, password)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSignIn.isEnabled = !loading
        binding.buttonSignInWithGoogle.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "LoginFragment"
    }
}