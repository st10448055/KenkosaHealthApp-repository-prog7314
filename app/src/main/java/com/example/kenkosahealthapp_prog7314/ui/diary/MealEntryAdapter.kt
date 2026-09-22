package com.example.kenkosahealthapp_prog7314.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.MealEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemMealEntryBinding

class MealEntryAdapter(
    private val onSaveServings: (mealId: String, newServings: Double) -> Unit,
    private val onDeleteClicked: (mealId: String) -> Unit
) : ListAdapter<MealEntryDto, MealEntryAdapter.MealViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealViewHolder(private val binding: ItemMealEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: MealEntryDto) {
            binding.textFoodName.text = entry.foodName
            binding.textCalories.text = "${entry.calories.toInt()} kcal"
            binding.editServings.setText(formatServings(entry.servings))

            binding.buttonSave.setOnClickListener {
                val newValue = binding.editServings.text.toString().toDoubleOrNull()
                if (newValue != null && newValue > 0 && newValue != entry.servings) {
                    onSaveServings(entry.mealId, newValue)
                }
            }

            binding.buttonDelete.setOnClickListener {
                onDeleteClicked(entry.mealId)
            }
        }

        private fun formatServings(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    class DiffCallback : DiffUtil.ItemCallback<MealEntryDto>() {
        override fun areItemsTheSame(oldItem: MealEntryDto, newItem: MealEntryDto) =
            oldItem.mealId == newItem.mealId

        override fun areContentsTheSame(oldItem: MealEntryDto, newItem: MealEntryDto) =
            oldItem == newItem
    }
}