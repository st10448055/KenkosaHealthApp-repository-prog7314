package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemFastEntryBinding


class FastEntryAdapter(
    private val onClick: (FastDto) -> Unit,
    private val onLongClick: (FastDto) -> Unit
) : ListAdapter<FastDto, FastEntryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemFastEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemFastEntryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fast: FastDto) {
            val context = binding.root.context
            binding.textDuration.text = FastTimeFormat.duration(context, fast.endTime - fast.startTime)
            binding.textRange.text = FastTimeFormat.range(context, fast.startTime, fast.endTime)

            binding.root.setOnClickListener { onClick(fast) }
            binding.root.setOnLongClickListener {
                onLongClick(fast)
                true
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<FastDto>() {
        override fun areItemsTheSame(oldItem: FastDto, newItem: FastDto) = oldItem.fastId == newItem.fastId
        override fun areContentsTheSame(oldItem: FastDto, newItem: FastDto) = oldItem == newItem
    }
}
