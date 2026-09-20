package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemExerciseBinding

class ExerciseAdapter : ListAdapter<ExerciseDto, ExerciseAdapter.ExerciseViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseDto) {
            binding.textExerciseName.text = exercise.name

            binding.textExerciseEquipment.text = if (exercise.equipment?.isNotEmpty() == true) {
                "Equipment: ${exercise.equipment.joinToString(", ")}"
            } else {
                "No equipment needed"
            }

            val plainDescription = Html.fromHtml(exercise.description, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .trim()

            binding.textExerciseDescription.text = plainDescription.ifBlank { "No description available" }


        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseDto>() {
        override fun areItemsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExerciseDto, newItem: ExerciseDto) =
            oldItem == newItem
    }
}