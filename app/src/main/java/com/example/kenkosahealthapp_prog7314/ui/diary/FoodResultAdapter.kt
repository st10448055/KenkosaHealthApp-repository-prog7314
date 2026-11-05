package com.example.kenkosahealthapp_prog7314.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.FoodItemDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemFoodResultBinding

// Shows the matches from a food search as a tappable list under the search bar.
// The tapped row is highlighted (same selected style as the meal type pills).
class FoodResultAdapter(
    private val onFoodClicked: (FoodItemDto) -> Unit
) : ListAdapter<FoodItemDto, FoodResultAdapter.ResultViewHolder>(DiffCallback()) {

    private var selectedId: String? = null

    // Highlights the row with this food id (null clears the highlight)
    fun setSelected(foodId: String?) {
        if (selectedId == foodId) return
        selectedId = foodId
        notifyDataSetChanged() // at most ~10 rows, so a full refresh is fine
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemFoodResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResultViewHolder(private val binding: ItemFoodResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(food: FoodItemDto) {
            binding.textResultName.text = food.name
            binding.textResultKcal.text = "${food.calories.toInt()} kcal / 100 g"
            // isSelected also reaches the child TextViews, which flip to white text
            binding.root.isSelected = food.foodId == selectedId
            binding.root.setOnClickListener { onFoodClicked(food) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FoodItemDto>() {
        override fun areItemsTheSame(old: FoodItemDto, new: FoodItemDto) = old.foodId == new.foodId
        override fun areContentsTheSame(old: FoodItemDto, new: FoodItemDto) = old == new
    }
}