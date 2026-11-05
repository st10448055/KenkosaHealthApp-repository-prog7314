package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentSsoBinding

//"Continue with email" screen shown before login — collects the user's email
// and passes it forward to the Login screen so it's pre-filled there.
class SsoFragment : Fragment() {

    private var _binding: FragmentSsoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSsoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate back to the previous screen (e.g. Start screen)
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonContinue.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            if (email.isEmpty()) {
                // Show inline validation error and stop navigation
                binding.editEmail.error = getString(R.string.auth_enter_email_continue)
                return@setOnClickListener
            }

            // Pass the entered email forward so the Login screen can pre-fill it
            findNavController().navigate(
                R.id.action_sso_to_login,
                bundleOf("email" to email)
            )
        }
        // Shortcut for new users to skip straight to account creation
        binding.textGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_sso_to_sign_up)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------