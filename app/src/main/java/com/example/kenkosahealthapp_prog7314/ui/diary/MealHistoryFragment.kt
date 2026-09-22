package com.example.kenkosahealthapp_prog7314.ui.diary

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
import com.example.kenkosahealthapp_prog7314.databinding.FragmentMealHistoryBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemMealSectionBinding
import com.example.kenkosahealthapp_prog7314.ui.addmenu.AddMenuBottomSheet
import kotlinx.coroutines.launch

class MealHistoryFragment : Fragment() {

    private var _binding: FragmentMealHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MealHistoryViewModel by viewModels()

    private lateinit var breakfastAdapter: MealEntryAdapter
    private lateinit var lunchAdapter: MealEntryAdapter
    private lateinit var dinnerAdapter: MealEntryAdapter
    private lateinit var snackAdapter: MealEntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollMealHistory.updatePadding(top = bars.top)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        setupSection(binding.sectionBreakfast, "Breakfast", MealType.BREAKFAST)
        setupSection(binding.sectionLunch, "Lunch", MealType.LUNCH)
        setupSection(binding.sectionDinner, "Dinner", MealType.DINNER)
        setupSection(binding.sectionSnack, "Snack", MealType.SNACK)

        setupWindowInsets()
        setupBottomNav()

        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time this screen becomes visible again — covers the
        // "came back from Log Food after adding an item" case.
        viewModel.loadTodaysMeals()
    }

    private fun setupSection(section: ItemMealSectionBinding, title: String, mealType: MealType) {
        section.textSectionTitle.text = title

        val adapter = MealEntryAdapter(
            onSaveServings = { mealId, newServings -> viewModel.updateServings(mealId, newServings) },
            onDeleteClicked = { mealId -> viewModel.deleteMeal(mealId) }
        )

        section.recyclerMealEntries.layoutManager = LinearLayoutManager(requireContext())
        section.recyclerMealEntries.adapter = adapter

        section.buttonAddToSection.setOnClickListener {
            navigateToLogFood(mealType)
        }

        when (mealType) {
            MealType.BREAKFAST -> breakfastAdapter = adapter
            MealType.LUNCH -> lunchAdapter = adapter
            MealType.DINNER -> dinnerAdapter = adapter
            MealType.SNACK -> snackAdapter = adapter
        }
    }

    private fun navigateToLogFood(mealType: MealType) {
        val bundle = Bundle().apply {
            putString("mealType", mealType.apiValue)
        }
        findNavController().navigate(
            com.example.kenkosahealthapp_prog7314.R.id.action_mealHistory_to_logFood,
            bundle
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.errorMessage != null) {
                        binding.textError.visibility = View.VISIBLE
                        binding.textError.text = state.errorMessage
                    } else {
                        binding.textError.visibility = View.GONE
                    }

                    bindSection(binding.sectionBreakfast, breakfastAdapter, state.breakfast)
                    bindSection(binding.sectionLunch, lunchAdapter, state.lunch)
                    bindSection(binding.sectionDinner, dinnerAdapter, state.dinner)
                    bindSection(binding.sectionSnack, snackAdapter, state.snack)
                }
            }
        }
    }

    private fun bindSection(
        section: ItemMealSectionBinding,
        adapter: MealEntryAdapter,
        items: List<com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto>
    ) {
        adapter.submitList(items)
        section.textEmptySection.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        section.recyclerMealEntries.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}