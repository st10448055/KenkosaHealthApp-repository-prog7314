package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentExerciseListBinding
import com.example.kenkosahealthapp_prog7314.ui.addmenu.AddMenuBottomSheet
import kotlinx.coroutines.launch

class ExerciseListFragment : Fragment() {

    private var _binding: FragmentExerciseListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseListViewModel by viewModels()
    private lateinit var adapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupBottomNav()

        val categoryId = arguments?.getInt("categoryId") ?: -1
        val categoryName = arguments?.getString("categoryName") ?: "Exercises"

        binding.textCategoryTitle.text = categoryName
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        adapter = ExerciseAdapter { exercise ->
            ExerciseDetailBottomSheet(exercise).show(parentFragmentManager, "exerciseDetail")
        }
        binding.recyclerExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExercises.adapter = adapter

        binding.editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.search(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        observeState()

        if (categoryId != -1) {
            viewModel.loadExercises(categoryId)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollExercises.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_fitness_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_fitness_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_fitness_to_settings) }
            fabAdd.setOnClickListener {
                AddMenuBottomSheet().show(childFragmentManager, "AddMenuBottomSheet")
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ExerciseListUiState.Loading -> {
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.textError.visibility = View.GONE
                        }
                        is ExerciseListUiState.Success -> {
                            binding.progressLoading.visibility = View.GONE
                            adapter.submitList(state.filtered)
                        }
                        is ExerciseListUiState.Error -> {
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