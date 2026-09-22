package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.databinding.BottomSheetExerciseDetailBinding

// Bottom sheet shown when an exercise is tapped, displaying its name, required
// equipment, and a plain-text description (the description arrives as HTML
// from the API, so it's stripped down before display).
class ExerciseDetailBottomSheet(private val exercise: ExerciseDto) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetExerciseDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textDetailName.text = exercise.name

        // Show a friendly fallback when no equipment is required, rather than
        // leaving the field blank.
        binding.textDetailEquipment.text = if (exercise.equipment.isNotEmpty()) {
            exercise.equipment.joinToString(", ")
        } else {
            "No equipment needed"
        }

        // Strip HTML markup out of the API-provided description down to plain text.
        val plainDescription = Html.fromHtml(exercise.description, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .trim()

        binding.textDetailDescription.text = plainDescription.ifBlank { "No description available" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------