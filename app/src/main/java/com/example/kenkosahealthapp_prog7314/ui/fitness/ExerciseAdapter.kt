package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemExerciseBinding

class ExerciseAdapter(
    private val onClick: (ExerciseDto) -> Unit
) : ListAdapter<ExerciseDto, ExerciseAdapter.ExerciseViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExerciseViewHolder(
        private val binding: ItemExerciseBinding,
        private val onClick: (ExerciseDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseDto) {
            binding.textExerciseName.text = exercise.name
            binding.root.setOnClickListener { onClick(exercise) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseDto>() {
        override fun areItemsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem == newItem
    }
}