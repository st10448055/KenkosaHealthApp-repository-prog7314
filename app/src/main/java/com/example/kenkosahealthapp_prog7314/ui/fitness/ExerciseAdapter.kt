package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemExerciseBinding

// RecyclerView adapter for the list of exercises within a chosen category.
class ExerciseAdapter(
    // Invoked when an exercise row is tapped -> opens its detail bottom sheet.
    private val onClick: (ExerciseDto) -> Unit
) : ListAdapter<ExerciseDto, ExerciseAdapter.ExerciseViewHolder>(DiffCallback()) {

    // Inflates the exercise row layout and wraps it in a ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Holds references to the views for a single exercise row.
    // (A plain class here, not `inner`, since it takes onClick directly rather
    // than closing over the outer adapter instance.)
    class ExerciseViewHolder(
        private val binding: ItemExerciseBinding,
        private val onClick: (ExerciseDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // Populates the row's name text and wires up the click callback.
        fun bind(exercise: ExerciseDto) {
            binding.textExerciseName.text = exercise.name
            binding.root.setOnClickListener { onClick(exercise) }
        }
    }

    // Tells ListAdapter how to diff old vs. new exercise lists efficiently.
    class DiffCallback : DiffUtil.ItemCallback<ExerciseDto>() {
        // Two items are the "same" row if they share the same exercise ID.
        override fun areItemsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem.id == newItem.id

        // Once matched by ID, check whether their full contents are still identical.
        override fun areContentsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem == newItem
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------