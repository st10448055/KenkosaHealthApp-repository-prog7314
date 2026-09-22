package com.example.kenkosahealthapp_prog7314.ui.knowhealth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.databinding.ItemHealthArticleBinding

// Adapter for displaying health article cards in a RecyclerView list
class HealthArticleAdapter(
    private val onClick: (HealthArticle) -> Unit
) : ListAdapter<HealthArticle, HealthArticleAdapter.ViewHolder>(DiffCallback) {

    // Inflates the layout item view binding for each article card
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemHealthArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    // Binds article data to the view holder at the given position
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    // ViewHolder class managing individual article item views
    inner class ViewHolder(private val binding: ItemHealthArticleBinding) : RecyclerView.ViewHolder(binding.root) {

        // Binds card visuals including gradient background, title, and optional thumbnail image
        fun bind(article: HealthArticle) {
            val context = binding.root.context

            // Apply dynamic gradient background based on article theme colors
            binding.viewGradient.background = HealthArticles.gradient(context, article)
            binding.textTitle.setText(article.titleRes)

            // Show image overlay and scrim if image resource exists, otherwise hide them
            val image = article.imageRes
            if (image != null) {
                binding.imageBackground.setImageResource(image)
                binding.imageBackground.visibility = View.VISIBLE
                binding.viewScrim.visibility = View.VISIBLE
            } else {
                binding.imageBackground.visibility = View.GONE
                binding.viewScrim.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(article) }
        }
    }

    // Item diffing logic for efficient list updates
    object DiffCallback : DiffUtil.ItemCallback<HealthArticle>() {
        override fun areItemsTheSame(oldItem: HealthArticle, newItem: HealthArticle) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HealthArticle, newItem: HealthArticle) = oldItem == newItem
    }
}
//---------------------------------------END OF FILE-------------------------------------------------