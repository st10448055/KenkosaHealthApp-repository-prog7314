package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentStartBinding



 // The app's very first screen (landing/onboarding entry point), offering the user
 // a choice to get started (new account flow) or go straight to login.
class StartFragment : Fragment() {

    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Get Started" leads into the SSO/email-entry flow for new users
        binding.buttonGetStarted.setOnClickListener {
            findNavController().navigate(
                com.example.kenkosahealthapp_prog7314.R.id.action_start_to_sso
            )
        }

        // Returning users can skip straight to the login screen
        binding.textGoToLogin.setOnClickListener {
            findNavController().navigate(
                com.example.kenkosahealthapp_prog7314.R.id.action_start_to_login
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------