package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemMoodEntryBinding

/** "Recent Moods" list on the Wellness screen. Tap = open details / edit, long-press = delete. */
class MoodEntryAdapter(
    private val onClick: (MoodEntryDto) -> Unit,
    private val onLongClick: (MoodEntryDto) -> Unit
) : ListAdapter<MoodEntryDto, MoodEntryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemMoodEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemMoodEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: MoodEntryDto) {
            val context = binding.root.context
            val mood = Mood.fromKey(entry.mood) ?: Mood.NEUTRAL

            binding.imageMood.setImageResource(mood.iconRes)
            binding.textMoodTitle.setText(mood.labelRes)
            binding.textMoodTime.text = DateUtils.formatDateTime(
                context,
                entry.loggedAt,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
            )

            val feelings = entry.feelings
                .mapNotNull { Feeling.fromKey(it) }
                .joinToString(", ") { context.getString(it.labelRes) }
            binding.textMoodFeelings.text = feelings
            binding.textMoodFeelings.visibility =
                if (feelings.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            binding.root.setOnClickListener { onClick(entry) }
            binding.root.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MoodEntryDto>() {
        override fun areItemsTheSame(oldItem: MoodEntryDto, newItem: MoodEntryDto) =
            oldItem.moodId == newItem.moodId

        override fun areContentsTheSame(oldItem: MoodEntryDto, newItem: MoodEntryDto) =
            oldItem == newItem
    }
}
