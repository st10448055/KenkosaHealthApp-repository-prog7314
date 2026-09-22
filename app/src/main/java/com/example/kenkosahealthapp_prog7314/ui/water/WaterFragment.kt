package com.example.kenkosahealthapp_prog7314.ui.water

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentWaterBinding
import kotlinx.coroutines.launch

class WaterFragment : Fragment() {

    // Binding gives direct access to the views in fragment_water.xml.
    // It is nullable because the Fragment's view can be destroyed separately from the Fragment.
    private var _binding: FragmentWaterBinding? = null
    private val binding get() = _binding!!

    // The ViewModel handles the water-tracking data and updates the UI state.
    private val viewModel: WaterViewModel by viewModels()

    // Stores the cup ImageViews that are created dynamically.
    private val cupViews = mutableListOf<ImageView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the Fragment layout using View Binding.
        _binding = FragmentWaterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Return to the previous screen when the back button is clicked.
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        // Start observing changes to the water-tracking state.
        observeState()
    }

    private fun buildCups(targetCups: Int) {
        // Only create the cups once, even if the ViewModel sends another state update.
        if (cupViews.isNotEmpty()) return

        // Arrange the cups into four columns inside the GridLayout.
        binding.gridCups.columnCount = 4

        // Get the appropriate colour from the app's current theme.
        val cupTint = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.tracking_text_primary)
        )

        // Create one cup ImageView for every cup in the user's daily target.
        for (i in 1..targetCups) {
            val cup = ImageView(requireContext()).apply {

                // Temporary placeholder image for each cup.
                setImageResource(android.R.drawable.ic_menu_report_image)

                // Apply the selected tint colour to the cup.
                imageTintList = cupTint

                // Set the size and spacing of each cup in the grid.
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    setMargins(12, 12, 12, 12)
                }

                // When a cup is clicked, update the number of cups consumed.
                setOnClickListener { viewModel.setCups(i) }
            }

            // Keep a reference to the cup so its appearance can be updated later.
            cupViews.add(cup)
            binding.gridCups.addView(cup)
        }
    }

    private fun updateCupVisuals(cupsConsumed: Int) {
        // Check each cup to determine whether it should appear filled or empty.
        cupViews.forEachIndexed { index, cup ->
            val cupNumber = index + 1

            // Consumed cups are fully visible, while remaining cups are faded.
            cup.alpha = if (cupNumber <= cupsConsumed) 1.0f else 0.3f
        }
    }

    private fun observeState() {
        // Use the Fragment's lifecycle to safely collect the StateFlow.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // React whenever the ViewModel's UI state changes.
                viewModel.uiState.collect { state ->
                    when (state) {

                        is WaterUiState.Loading -> {
                            // Show the loading indicator while the data is being retrieved.
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.textError.visibility = View.GONE
                        }

                        is WaterUiState.Success -> {
                            // Hide the loading indicator once the data has loaded.
                            binding.progressLoading.visibility = View.GONE

                            // Display the number of cups consumed and the user's target.
                            binding.textWaterSummary.text = getString(
                                R.string.water_summary,
                                state.cupsConsumed,
                                state.targetCups
                            )

                            // Create the correct number of cups based on the target.
                            buildCups(state.targetCups)

                            // Update which cups appear filled.
                            updateCupVisuals(state.cupsConsumed)
                        }

                        is WaterUiState.Error -> {
                            // Hide loading and show the error message to the user.
                            binding.progressLoading.visibility = View.GONE
                            binding.textError.visibility = View.VISIBLE
                            binding.textError.text = state.message
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clear the binding when the view is destroyed to prevent memory leaks.
        _binding = null
    }
}