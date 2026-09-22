package com.example.kenkosahealthapp_prog7314.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.databinding.FragmentLogFoodBinding
import kotlinx.coroutines.launch
import com.example.kenkosahealthapp_prog7314.R

class LogFoodFragment : Fragment() {

    private var _binding: FragmentLogFoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogFoodViewModel by viewModels()

    private lateinit var mealTypeViews: Map<MealType, android.widget.TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        val incomingMealType = arguments?.getString("mealType")
        val initialMealType = MealType.entries.find { it.apiValue == incomingMealType } ?: MealType.BREAKFAST

        // --- Meal type selector: fixed buttons, no free text, matches your design mockup ---
        mealTypeViews = mapOf(
            MealType.BREAKFAST to binding.mealTypeBreakfast,
            MealType.LUNCH to binding.mealTypeLunch,
            MealType.DINNER to binding.mealTypeDinner,
            MealType.SNACK to binding.mealTypeSnack
        )
        mealTypeViews.forEach { (type, view) ->
            view.setOnClickListener { selectMealType(type) }
        }
        selectMealType(initialMealType)

        // --- Search: text search ---
        binding.buttonSearch.setOnClickListener {
            val query = binding.editSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) viewModel.searchByText(query)
        }

        binding.buttonScanBarcode.setOnClickListener {
            findNavController().navigate(R.id.action_logFood_to_barcodeScanner)
        }

// Listen for the barcode result coming back from BarcodeScannerFragment
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>("scanned_barcode")
            ?.observe(viewLifecycleOwner) { barcode ->
                viewModel.searchByBarcode(barcode)
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_barcode")
            }

        // --- Servings input ---
        binding.editServings.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                s.toString().toDoubleOrNull()?.let { viewModel.updateServings(it) }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.buttonAddEntry.setOnClickListener { viewModel.logCurrentFood() }

        observeSearchState()
        observeLogState()
    }

    private fun selectMealType(type: MealType) {
        viewModel.selectMealType(type)
        mealTypeViews.forEach { (mealType, view) ->
            view.isSelected = (mealType == type) // relies on a selector drawable, same idea as AddSleepFragment's quality buttons
        }
    }

    private fun observeSearchState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect { state ->
                    when (state) {
                        is FoodSearchState.Idle -> {
                            binding.progressSearch.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                        }
                        is FoodSearchState.Loading -> {
                            binding.progressSearch.visibility = View.VISIBLE
                            binding.textSearchError.visibility = View.GONE
                        }
                        is FoodSearchState.Found -> {
                            binding.progressSearch.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.VISIBLE
                            binding.textFoodName.text = state.food.name
                            binding.textCalories.text = "${state.food.calories.toInt()} kcal"
                            binding.textProtein.text = "Protein: ${state.food.proteinG}g"
                            binding.textCarbs.text = "Carbs: ${state.food.carbsG}g"
                            binding.textFat.text = "Fat: ${state.food.fatG}g"
                        }
                        is FoodSearchState.Error -> {
                            binding.progressSearch.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                            binding.textSearchError.visibility = View.VISIBLE
                            binding.textSearchError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun observeLogState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logState.collect { state ->
                    when (state) {
                        is LogMealState.Idle -> Unit
                        is LogMealState.Saving -> binding.buttonAddEntry.isEnabled = false
                        is LogMealState.Success -> {
                            binding.buttonAddEntry.isEnabled = true
                            findNavController().navigateUp()
                        }
                        is LogMealState.Error -> {
                            binding.buttonAddEntry.isEnabled = true
                            binding.textSearchError.visibility = View.VISIBLE
                            binding.textSearchError.text = state.message
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