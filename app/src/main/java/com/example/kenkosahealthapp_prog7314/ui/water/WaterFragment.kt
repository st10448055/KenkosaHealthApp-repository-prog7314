package com.example.kenkosahealthapp_prog7314.ui.water

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentWaterBinding
import kotlinx.coroutines.launch

class WaterFragment : Fragment() {

    private var _binding: FragmentWaterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WaterViewModel by viewModels()
    private val cupViews = mutableListOf<ImageView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        observeState()
    }

    private fun buildCups(targetCups: Int) {
        if (cupViews.isNotEmpty()) return // only build once, even if state updates again

        binding.gridCups.columnCount = 4

        for (i in 1..targetCups) {
            val cup = ImageView(requireContext()).apply {
                setImageResource(android.R.drawable.ic_menu_report_image) // placeholder icon; swap for a real cup drawable
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    setMargins(12, 12, 12, 12)
                }
                setOnClickListener { viewModel.setCups(i) }
            }
            cupViews.add(cup)
            binding.gridCups.addView(cup)
        }
    }

    private fun updateCupVisuals(cupsConsumed: Int) {
        cupViews.forEachIndexed { index, cup ->
            val cupNumber = index + 1
            cup.alpha = if (cupNumber <= cupsConsumed) 1.0f else 0.3f // filled vs empty look
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is WaterUiState.Loading -> {
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.textError.visibility = View.GONE
                        }
                        is WaterUiState.Success -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.textWaterSummary.text =
                                "${state.cupsConsumed} / ${state.targetCups} cups today"
                            buildCups(state.targetCups)
                            updateCupVisuals(state.cupsConsumed)
                        }
                        is WaterUiState.Error -> {
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
        _binding = null
    }
}