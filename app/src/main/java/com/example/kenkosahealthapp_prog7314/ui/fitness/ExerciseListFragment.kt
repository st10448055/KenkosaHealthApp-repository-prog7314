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

// Shows the list of exercises within a single category (e.g. "Cardio", "Strength"),
// with a search box to filter by name and a tap target that opens exercise details.
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

        // Which category we're showing; -1/"Exercises" are safe fallbacks if
        // this screen is somehow opened without arguments.
        val categoryId = arguments?.getInt("categoryId") ?: -1
        val categoryName = arguments?.getString("categoryName") ?: "Exercises"

        binding.textCategoryTitle.text = categoryName
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        // Tapping an exercise opens its details in a bottom sheet.
        adapter = ExerciseAdapter { exercise ->
            ExerciseDetailBottomSheet(exercise).show(parentFragmentManager, "exerciseDetail")
        }
        binding.recyclerExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExercises.adapter = adapter

        // Live-filters the list as the user types in the search box.
        binding.editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.search(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        observeState()

        // Only fetch exercises if we actually got a valid category ID.
        if (categoryId != -1) {
            viewModel.loadExercises(categoryId)
        }
    }

    // Applies system bar/cutout insets as padding so content isn't drawn under
    // the status bar and the bottom nav isn't drawn under gesture/nav bars.
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

    // Collects the ViewModel's exercise-list state and updates loading/error/content UI.
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
                            // Always render the filtered list, which defaults to
                            // the full list when there's no active search query.
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
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------