package com.example.kenkosahealthapp_prog7314.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentFeaturePlaceholderBinding

// Generic placeholder screen used for features that aren't built yet.
// Shows a title (passed in via arguments) and a back button, so nav destinations
// can point somewhere real before the actual feature UI is implemented.
class FeaturePlaceholderFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentFeaturePlaceholderBinding? = null

    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeaturePlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Falls back to a generic "Feature" label if no title was passed in
        val title = arguments?.getString("featureTitle") ?: "Feature"
        binding.textFeatureTitle.text = title

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks by releasing the binding reference
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------