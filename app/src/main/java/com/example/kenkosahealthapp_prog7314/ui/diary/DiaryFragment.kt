package com.example.kenkosahealthapp_prog7314.ui.diary

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.FragmentDiaryBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemMealSectionBinding
import com.example.kenkosahealthapp_prog7314.ui.water.WaterUiState
import com.example.kenkosahealthapp_prog7314.ui.water.WaterViewModel
import com.example.kenkosahealthapp_prog7314.ui.wellness.MoodUiState
import com.example.kenkosahealthapp_prog7314.ui.wellness.MoodViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealHistoryViewModel by viewModels()
    private val waterViewModel: WaterViewModel by viewModels()
    private val moodViewModel: MoodViewModel by viewModels()
    private val notesViewModel: NotesViewModel by viewModels()

    private lateinit var breakfastAdapter: MealEntryAdapter
    private lateinit var lunchAdapter: MealEntryAdapter
    private lateinit var dinnerAdapter: MealEntryAdapter
    private lateinit var snackAdapter: MealEntryAdapter

    private val cupViews = mutableListOf<ImageView>()
    private var isTypingNote = false // suppresses re-render fighting the user's cursor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        setupMealSections()
        setupNotes()

        observeMeals()
        observeWater()
        observeMood()
        observeNotes()
    }

    override fun onResume() {
        super.onResume()
        // Refresh everything each time Diary becomes visible, so edits made
        // elsewhere (FAB water screen, Wellness mood screen, Log Food) show up here.
        mealViewModel.loadTodaysMeals()
        waterViewModel.loadWater()
        moodViewModel.loadMoods()
        notesViewModel.loadNote()
    }

    // ---------------------------------------------------------------- Meals

    private fun setupMealSections() {
        breakfastAdapter = buildMealAdapter()
        lunchAdapter = buildMealAdapter()
        dinnerAdapter = buildMealAdapter()
        snackAdapter = buildMealAdapter()

        configureSection(binding.sectionBreakfast, "Breakfast", MealType.BREAKFAST, breakfastAdapter)
        configureSection(binding.sectionLunch, "Lunch", MealType.LUNCH, lunchAdapter)
        configureSection(binding.sectionDinner, "Dinner", MealType.DINNER, dinnerAdapter)
        configureSection(binding.sectionSnack, "Snack", MealType.SNACK, snackAdapter)
    }

    private fun buildMealAdapter() = MealEntryAdapter(
        onSaveServings = { mealId, newServings -> mealViewModel.updateServings(mealId, newServings) },
        onDeleteClicked = { mealId -> mealViewModel.deleteMeal(mealId) }
    )

    private fun configureSection(
        section: ItemMealSectionBinding, title: String, mealType: MealType, adapter: MealEntryAdapter
    ) {
        section.textSectionTitle.text = title
        section.recyclerMealEntries.layoutManager = LinearLayoutManager(requireContext())
        section.recyclerMealEntries.adapter = adapter
        section.buttonAddToSection.setOnClickListener {
            val bundle = Bundle().apply { putString("mealType", mealType.apiValue) }
            findNavController().navigate(
                com.example.kenkosahealthapp_prog7314.R.id.action_global_logFood, bundle
            )
        }
    }

    private fun observeMeals() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mealViewModel.uiState.collect { state ->
                    bindMealSection(binding.sectionBreakfast, breakfastAdapter, state.breakfast)
                    bindMealSection(binding.sectionLunch, lunchAdapter, state.lunch)
                    bindMealSection(binding.sectionDinner, dinnerAdapter, state.dinner)
                    bindMealSection(binding.sectionSnack, snackAdapter, state.snack)
                }
            }
        }
    }

    private fun bindMealSection(
        section: ItemMealSectionBinding, adapter: MealEntryAdapter,
        items: List<com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto>
    ) {
        adapter.submitList(items)
        section.textEmptySection.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        section.recyclerMealEntries.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    // ---------------------------------------------------------------- Water

    private fun buildCups(targetCups: Int) {
        if (cupViews.isNotEmpty()) return

        for (i in 1..targetCups) {
            val cup = ImageView(requireContext()).apply {
                setImageResource(android.R.drawable.ic_menu_report_image)
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 100
                    height = 100
                    setMargins(10, 10, 10, 10)
                }
                setOnClickListener { waterViewModel.setCups(i) }
            }
            cupViews.add(cup)
            binding.gridCups.addView(cup)
        }
    }

    private fun updateCupVisuals(cupsConsumed: Int) {
        cupViews.forEachIndexed { index, cup ->
            cup.alpha = if (index + 1 <= cupsConsumed) 1.0f else 0.3f
        }
    }

    private fun observeWater() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                waterViewModel.uiState.collect { state ->
                    if (state is WaterUiState.Success) {
                        binding.textWaterSummary.text = "${state.cupsConsumed} / ${state.targetCups} cups today"
                        buildCups(state.targetCups)
                        updateCupVisuals(state.cupsConsumed)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------- Mood

    private fun observeMood() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                moodViewModel.uiState.collect { state ->
                    if (state is MoodUiState.Success) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val todaysMoods = state.entries.filter { entry ->
                            val entryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Date(entry.loggedAt))
                            entryDate == today
                        }
                        renderMoodEntries(todaysMoods)
                    }
                }
            }
        }
    }

    private fun renderMoodEntries(entries: List<MoodEntryDto>) {
        binding.containerMoodEntries.removeAllViews()

        if (entries.isEmpty()) {
            binding.textNoMoodToday.visibility = View.VISIBLE
            return
        }
        binding.textNoMoodToday.visibility = View.GONE

        entries.forEach { entry ->
            val row = TextView(requireContext()).apply {
                text = "${entry.mood} — ${entry.feelings.joinToString(", ")}"
                setPadding(16, 16, 16, 16)
                setBackgroundColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    // TODO: navigate to your teammate's Log Mood / edit screen with entry.id,
                    // once you tell me the destination/nav action it uses.
                }
            }
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            row.layoutParams = params
            binding.containerMoodEntries.addView(row)
        }
    }

    // ---------------------------------------------------------------- Notes

    private fun setupNotes() {
        binding.editNotes.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                notesViewModel.saveNote(binding.editNotes.text.toString())
            }
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notesViewModel.uiState.collect { state ->
                    if (state is NotesUiState.Success && !binding.editNotes.isFocused) {
                        // Only overwrite the text field when the user ISN'T currently
                        // typing in it, so a background refresh never yanks their cursor.
                        binding.editNotes.setText(state.noteText)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cupViews.clear()
        _binding = null
    }
}