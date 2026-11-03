package com.example.kenkosahealthapp_prog7314.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentBiometricLockBinding
import com.example.kenkosahealthapp_prog7314.security.BiometricAvailability
import com.example.kenkosahealthapp_prog7314.security.BiometricHelper
import com.example.kenkosahealthapp_prog7314.security.BiometricPreferences
import com.google.firebase.auth.FirebaseAuth

// Lock screen shown when a signed-in user opens the app (or returns after the re-lock timeout)
// and has biometric login enabled. The user must pass fingerprint/face verification to continue,
// or choose "Use password instead" which signs them out and returns to the normal login.
class BiometricLockFragment : Fragment() {

    private var _binding: FragmentBiometricLockBinding? = null
    private val binding get() = _binding!!

    // true  = app was re-locked while in use -> unlocking returns to the previous screen
    // false = cold start -> unlocking goes to Home
    private val isRelock: Boolean
        get() = arguments?.getBoolean(ARG_RELOCK, false) ?: false

    // Stops the prompt re-appearing on every rotation / resume once it has been handled.
    private var promptShown = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiometricLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Lock screen shown (relock=$isRelock)")

        binding.buttonUnlock.setOnClickListener { showPrompt() }
        binding.buttonUsePassword.setOnClickListener { fallbackToPasswordLogin() }

        // Automatically show the system prompt the first time the screen appears.
        if (savedInstanceState == null) showPrompt()
    }

    // Shows the system fingerprint/face dialog, after checking the hardware is still usable
    // (e.g. the user may have removed all fingerprints in system settings since enabling).
    private fun showPrompt() {
        if (promptShown) return

        val availability = BiometricHelper.checkAvailability(requireActivity())
        if (availability != BiometricAvailability.AVAILABLE) {
            Log.w(TAG, "Biometrics no longer available: $availability")
            showMessage(getString(R.string.biometric_unavailable_now))
            // Disable the setting so the user is not locked out of their own app.
            FirebaseAuth.getInstance().currentUser?.uid?.let {
                BiometricPreferences(requireContext()).setEnabled(it, false)
            }
            unlock()
            return
        }

        promptShown = true
        BiometricHelper.authenticate(
            fragment = this,
            title = getString(R.string.biometric_prompt_title),
            subtitle = getString(R.string.biometric_prompt_subtitle),
            negativeText = getString(R.string.biometric_use_password),
            onSuccess = { unlock() },
            onCancelled = {
                promptShown = false
                fallbackToPasswordLogin()
            },
            onError = { message ->
                // e.g. too many attempts -> temporary lockout. Keep the user on this screen.
                promptShown = false
                showMessage(message)
            }
        )
    }

    // Verification passed: continue into the app.
    private fun unlock() {
        if (_binding == null) return
        Log.i(TAG, "Unlocked")
        val nav = findNavController()
        if (isRelock) {
            nav.popBackStack()
        } else {
            nav.navigate(
                R.id.action_biometric_lock_to_home
            )
        }
    }

    // Signs out and shows the regular login screen (password or Google).
    private fun fallbackToPasswordLogin() {
        Log.i(TAG, "User chose password fallback - signing out")
        FirebaseAuth.getInstance().signOut()
        findNavController().navigate(
            R.id.startFragment,
            null,
            navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
        )
    }

    private fun showMessage(message: String) {
        binding.textLockMessage.text = message
        binding.textLockMessage.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "BiometricLock"
        const val ARG_RELOCK = "relock"
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------
