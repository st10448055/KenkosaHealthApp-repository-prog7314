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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentLogFoodBinding
import kotlinx.coroutines.launch
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.ui.settings.UnitFormatter

// Screen for logging a food item to the diary — pick a meal type, search by text
// or barcode, review the matched food's macros, adjust servings, then save.
class LogFoodFragment : Fragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: FragmentLogFoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogFoodViewModel by viewModels()

    // Maps each meal type to its corresponding selector button in the layout
    private lateinit var mealTypeViews: Map<MealType, android.widget.TextView>

    // Search result list under the search bar
    private lateinit var resultAdapter: FoodResultAdapter

    // The food currently shown in the details card (null when none is selected)
    private var shownFood: FoodItemDto? = null

    // Set when the user taps a result, so the screen scrolls down to the servings box once
    private var scrollToDetailsOnNextFound = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        // Pre-select the meal type passed in as an argument (e.g. tapped "Add" under Lunch),
        // defaulting to Breakfast if nothing was passed
        val incomingMealType = arguments?.getString("mealType")
        val initialMealType = MealType.entries.find { it.apiValue == incomingMealType } ?: MealType.BREAKFAST

        // Meal type selector
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

        // Search results list: tapping a row selects that food
        resultAdapter = FoodResultAdapter { food ->
            scrollToDetailsOnNextFound = true
            viewModel.selectFood(food)
        }
        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = resultAdapter

        // Search: text search
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

        // Servings input
        binding.editServings.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                s.toString().toDoubleOrNull()?.let {
                    viewModel.updateServings(it)
                    // Keep the shown totals in step with the servings typed
                    shownFood?.let { food -> showFood(food) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.buttonAddEntry.setOnClickListener { viewModel.logCurrentFood() }

        observeSearchState()
        observeResults()
        observeLogState()
    }

    // Fills the details card with the selected food, scaled by the servings entered
    private fun showFood(food: FoodItemDto) {
        val servings = viewModel.selectedServings
        val ctx = requireContext()
        binding.textFoodName.text = food.name
        binding.textCalories.text = "${(food.calories * servings).toInt()} kcal"
        binding.textProtein.text = getString(R.string.logfood_macro_protein, UnitFormatter.mass(ctx, food.proteinG * servings))
        binding.textCarbs.text = getString(R.string.logfood_macro_carbs, UnitFormatter.mass(ctx, food.carbsG * servings))
        binding.textFat.text = getString(R.string.logfood_macro_fat, UnitFormatter.mass(ctx, food.fatG * servings))
    }

    // Shows or hides the results list as the search results change
    private fun observeResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { foods ->
                    resultAdapter.submitList(foods)
                    val visibility = if (foods.isEmpty()) View.GONE else View.VISIBLE
                    binding.textResultsHeader.visibility = visibility
                    binding.recyclerResults.visibility = visibility
                }
            }
        }
    }

    // Updates the selected meal type in the ViewModel and toggles the selected visual state of each button
    private fun selectMealType(type: MealType) {
        viewModel.selectMealType(type)
        mealTypeViews.forEach { (mealType, view) ->
            view.isSelected = (mealType == type) // relies on a selector drawable, same idea as AddSleepFragment's quality buttons
        }
    }

    // Observes the food search result and updates the loading/result/error UI accordingly
    private fun observeSearchState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect { state ->
                    when (state) {
                        is FoodSearchState.Idle -> {
                            shownFood = null
                            binding.progressSearch.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                            resultAdapter.setSelected(null)
                        }
                        is FoodSearchState.Loading -> {
                            shownFood = null
                            binding.progressSearch.visibility = View.VISIBLE
                            binding.textSearchError.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                            resultAdapter.setSelected(null)
                        }
                        is FoodSearchState.Found -> {
                            binding.progressSearch.visibility = View.GONE
                            binding.textSearchError.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.VISIBLE
                            shownFood = state.food
                            showFood(state.food)
                            // Highlight the picked row (does nothing after a barcode scan)
                            resultAdapter.setSelected(state.food.foodId)
                            if (scrollToDetailsOnNextFound) {
                                scrollToDetailsOnNextFound = false
                                // Wait for the card to be laid out, then bring the servings box into view
                                binding.scrollRoot.post {
                                    _binding?.let { it.scrollRoot.smoothScrollTo(0, it.groupFoodResult.top) }
                                }
                            }
                        }
                        is FoodSearchState.Error -> {
                            shownFood = null
                            binding.progressSearch.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                            binding.textSearchError.visibility = View.VISIBLE
                            binding.textSearchError.text = state.message
                        }
                        is FoodSearchState.Results -> {
                            // The list itself is drawn by observeResults(); nothing is picked yet
                            shownFood = null
                            binding.progressSearch.visibility = View.GONE
                            binding.textSearchError.visibility = View.GONE
                            binding.groupFoodResult.visibility = View.GONE
                            resultAdapter.setSelected(null)
                        }
                    }
                }
            }
        }
    }

    // Observes the save/log result, disabling the add button while saving and navigating back on success
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
        // Prevent memory leaks by releasing the binding reference
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------