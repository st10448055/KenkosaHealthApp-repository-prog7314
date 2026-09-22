package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

// Login screen fragment - handles email/password login, Google sign-in, and navigation
// to sign-up. Also listens to AuthViewModel's state to show loading/errors and navigate on success.
class LoginFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentLoginBinding? = null

    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }

    // Handles the result of the Google Sign-In activity, extracting the ID token
    // and passing it to the ViewModel to complete Firebase authentication
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    showError("Google sign in was cancelled or failed")
                }
            } catch (e: ApiException) {
                showError("Google sign in failed (code ${e.statusCode})")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill the email field if it was passed in (e.g. from the sign-up or SSO screen)
        arguments?.getString("email")?.takeIf { it.isNotEmpty() }?.let {
            binding.editEmail.setText(it)
        }

        binding.buttonSignIn.setOnClickListener { attemptLogin() }

        binding.buttonSignInWithGoogle.setOnClickListener {
            googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
        }

        binding.textGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_sign_up)
        }

        binding.textUseFingerprint.setOnClickListener {
            showError("Fingerprint sign-in isn't set up yet")
        }

        // Observe auth state only while the view is at least STARTED, to avoid
        // updating views that don't exist yet or have already been destroyed
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> setLoading(true)
                        is AuthUiState.Success -> {
                            setLoading(false)
                            // Login succeeded, move on to the home screen
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
    // Validates the entered credentials locally before delegating the actual sign-in to the ViewModel
    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password")
            return
        }
        binding.textError.visibility = View.GONE
        viewModel.login(email, password)
    }

    // Toggles the progress bar and disables the sign-in button while a request is in flight
    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSignIn.isEnabled = !loading
    }

    // Displays an inline error message below the form
    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks by releasing the binding reference
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------