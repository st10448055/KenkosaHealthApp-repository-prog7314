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

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonContinue.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.editEmail.error = "Enter your email to continue"
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_sso_to_login,
                bundleOf("email" to email)
            )
        }

        binding.textGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_sso_to_sign_up)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}