package com.example.kenkosahealthapp_prog7314.ui.fitness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.ExerciseCategoryDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemExerciseCategoryBinding

class CategoryAdapter(
    private val onCategoryClicked: (ExerciseCategoryDto) -> Unit
) : ListAdapter<ExerciseCategoryDto, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemExerciseCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemExerciseCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ExerciseCategoryDto) {
            binding.textCategoryName.text = category.name
            binding.root.setOnClickListener { onCategoryClicked(category) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseCategoryDto>() {
        override fun areItemsTheSame(oldItem: ExerciseCategoryDto, newItem: ExerciseCategoryDto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExerciseCategoryDto, newItem: ExerciseCategoryDto) =
            oldItem == newItem
    }
}