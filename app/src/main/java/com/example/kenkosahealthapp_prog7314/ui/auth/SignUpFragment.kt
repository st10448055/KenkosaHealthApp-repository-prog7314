package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Sign-up screen fragment - collects new user details, creates the account via AuthViewModel,
// then signs the user back out and routes them to Login so their first proper login is what
// triggers the body metrics onboarding flow.

class SignUpFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentSignUpBinding? = null

    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSignUp.setOnClickListener { attemptSignUp() }
        binding.textGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_sign_up_to_login)
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
                            // The account is created. Sign out and send them to the Login screen (with their
                            // email filled in), so their first login is what shows the body metrics questions.
                            FirebaseAuth.getInstance().signOut()
                            viewModel.resetState()
                            findNavController().navigate(
                                R.id.action_sign_up_to_login,
                                androidx.core.os.bundleOf("email" to binding.editEmail.text.toString().trim())
                            )
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

    // Validates the sign-up form locally (required fields, matching passwords, min length)
    // before delegating account creation to the ViewModel
    private fun attemptSignUp() {
        val fullName = binding.editFullName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            showError("Passwords do not match")
            return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        // Split the full name into first name / surname for storage,
        // treating everything after the first space as the surname
        val parts = fullName.split(" ", limit = 2)
        val name = parts.getOrElse(0) { "" }
        val surname = parts.getOrElse(1) { "" }

        binding.textError.visibility = View.GONE
        viewModel.signUp(name, surname, email, password)
    }

    // Toggles the progress bar and disables the sign-up button while a request is in flight
    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSignUp.isEnabled = !loading
    }

    // Displays an inline error message below the form
    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    // Prevent memory leaks by releasing the binding reference
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------