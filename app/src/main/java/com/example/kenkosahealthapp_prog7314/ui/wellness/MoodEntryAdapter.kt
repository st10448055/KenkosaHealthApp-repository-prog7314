package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemMoodEntryBinding

// Adapter used for the "Recent Moods" list on the Wellness screen.
// Tapping opens the mood details/edit screen, while long-pressing allows deletion.
class MoodEntryAdapter(
    private val onClick: (MoodEntryDto) -> Unit,
    private val onLongClick: (MoodEntryDto) -> Unit
) : ListAdapter<MoodEntryDto, MoodEntryAdapter.ViewHolder>(DiffCallback) {

    // Create and inflate the layout used for each mood entry.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemMoodEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    // Bind the mood entry at this position to the ViewHolder.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    // Holds the views for one mood entry in the RecyclerView.
    inner class ViewHolder(
        private val binding: ItemMoodEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: MoodEntryDto) {
            val context = binding.root.context

            // Convert the stored mood key into the matching Mood enum.
            // Use NEUTRAL if the stored value cannot be recognised.
            val mood = Mood.fromKey(entry.mood) ?: Mood.NEUTRAL

            // Display the mood's icon and translated label.
            binding.imageMood.setImageResource(mood.iconRes)
            binding.textMoodTitle.setText(mood.labelRes)

            // Convert the stored timestamp into a readable date and time.
            binding.textMoodTime.text = DateUtils.formatDateTime(
                context,
                entry.loggedAt,
                DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_SHOW_TIME or
                        DateUtils.FORMAT_ABBREV_MONTH
            )

            // Convert the stored feeling keys into readable names.
            // Unknown feeling keys are ignored.
            val feelings = entry.feelings
                .mapNotNull { Feeling.fromKey(it) }
                .joinToString(", ") { context.getString(it.labelRes) }

            binding.textMoodFeelings.text = feelings

            // Hide the feelings text completely when no feelings were selected.
            binding.textMoodFeelings.visibility =
                if (feelings.isEmpty()) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

            // Open the mood entry when the user taps it.
            binding.root.setOnClickListener {
                onClick(entry)
            }

            // Trigger the delete action when the user long-presses the entry.
            // Returning true tells Android that the long-press event was handled.
            binding.root.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }
    }

    // Helps ListAdapter determine which items changed when the list is updated.
    object DiffCallback : DiffUtil.ItemCallback<MoodEntryDto>() {

        // Two entries represent the same item if they have the same unique mood ID.
        override fun areItemsTheSame(
            oldItem: MoodEntryDto,
            newItem: MoodEntryDto
        ) = oldItem.moodId == newItem.moodId

        // Check whether the actual contents of the two entries are unchanged.
        override fun areContentsTheSame(
            oldItem: MoodEntryDto,
            newItem: MoodEntryDto
        ) = oldItem == newItem
    }
}