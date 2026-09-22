package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.FastDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemFastEntryBinding


// RecyclerView adapter that renders the list of past (finished) fasts shown in the
// fasting history list. Uses ListAdapter so list diffing/animations are handled
// automatically via DiffCallback below.
class FastEntryAdapter(
    // Invoked when a row is tapped -> used to open that fast in the (read-only) details screen.
    private val onClick: (FastDto) -> Unit,

    // Invoked when a row is long-pressed -> used to trigger the delete confirmation dialog.
    private val onLongClick: (FastDto) -> Unit
) : ListAdapter<FastDto, FastEntryAdapter.ViewHolder>(DiffCallback) {

    // Inflates the single-row layout (item_fast_entry) and wraps it in a ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemFastEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    // Binds the FastDto at `position` to the given ViewHolder's views.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    // Holds references to the views for a single fast-history row.
    inner class ViewHolder(private val binding: ItemFastEntryBinding) : RecyclerView.ViewHolder(binding.root) {

        // Populates the row's text views and wires up click/long-click callbacks for this fast.
        fun bind(fast: FastDto) {
            val context = binding.root.context
            binding.textDuration.text = FastTimeFormat.duration(context, fast.endTime - fast.startTime)
            binding.textRange.text = FastTimeFormat.range(context, fast.startTime, fast.endTime)

            // Tapping a row opens it (view details).
            binding.root.setOnClickListener { onClick(fast) }

            // Long-pressing a row offers to delete it; return true to mark the long-click as consumed.
            binding.root.setOnLongClickListener {
                onLongClick(fast)
                true
            }
        }
    }

    // Tells ListAdapter how to efficiently diff old vs. new lists so only changed
    // rows are re-bound/animated instead of refreshing the whole list.
    object DiffCallback : DiffUtil.ItemCallback<FastDto>() {
        // Two items represent the "same" row if they share the same fast ID.
        override fun areItemsTheSame(oldItem: FastDto, newItem: FastDto) = oldItem.fastId == newItem.fastId
        // (relies on FastDto being a data class with structural equality).
        override fun areContentsTheSame(oldItem: FastDto, newItem: FastDto) = oldItem == newItem
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------