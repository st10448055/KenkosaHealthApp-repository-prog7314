package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseCategoryDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemExerciseCategoryBinding

// RecyclerView adapter for the grid of exercise categories on the Fitness screen.
class CategoryAdapter(
    // Invoked when a category tile is tapped -> navigates to that category's exercise list.
    private val onCategoryClicked: (ExerciseCategoryDto) -> Unit
) : ListAdapter<ExerciseCategoryDto, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    // Inflates the category tile layout and wraps it in a ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemExerciseCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Holds references to the views for a single category tile.
    inner class CategoryViewHolder(private val binding: ItemExerciseCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ExerciseCategoryDto) {
            binding.textCategoryName.text = category.name
            binding.root.setOnClickListener { onCategoryClicked(category) }
        }
    }

    // Tells ListAdapter how to diff old vs. new category lists efficiently.
    class DiffCallback : DiffUtil.ItemCallback<ExerciseCategoryDto>() {
        // Two items are the "same" tile if they share the same category ID.
        override fun areItemsTheSame(oldItem: ExerciseCategoryDto, newItem: ExerciseCategoryDto) =
            oldItem.id == newItem.id

        // Once matched by ID, check whether their full contents are still identical
        // (relies on ExerciseCategoryDto being a data class with structural equality).
        override fun areContentsTheSame(oldItem: ExerciseCategoryDto, newItem: ExerciseCategoryDto) =
            oldItem == newItem
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------