package com.example.kenkosahealthapp_prog7314.ui.knowhealth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.databinding.ItemHealthArticleBinding

/** The article cards. Tap one to read it. */
class HealthArticleAdapter(
    private val onClick: (HealthArticle) -> Unit
) : ListAdapter<HealthArticle, HealthArticleAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemHealthArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemHealthArticleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: HealthArticle) {
            val context = binding.root.context

            binding.viewGradient.background = HealthArticles.gradient(context, article)
            binding.textTitle.setText(article.titleRes)

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

    object DiffCallback : DiffUtil.ItemCallback<HealthArticle>() {
        override fun areItemsTheSame(oldItem: HealthArticle, newItem: HealthArticle) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HealthArticle, newItem: HealthArticle) = oldItem == newItem
    }
}
