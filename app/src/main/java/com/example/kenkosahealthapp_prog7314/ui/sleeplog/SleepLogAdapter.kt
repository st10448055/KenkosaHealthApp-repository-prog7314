
package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.SleepLogDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemSleepLogBinding
import java.util.Date

// Adapter used to display the user's recent sleep records in a RecyclerView.
// Tapping a record allows it to be edited, while a long press allows it to be deleted.
class SleepLogAdapter(
    // These functions are provided by the screen using the adapter.
    // They determine what should happen when a record is clicked or long-pressed.
    private val onClick: (SleepLogDto) -> Unit,
    private val onLongClick: (SleepLogDto) -> Unit
) : ListAdapter<SleepLogDto, SleepLogAdapter.ViewHolder>(DiffCallback) {

    // Creates a ViewHolder when the RecyclerView needs a new list item.
    // View Binding is used here to connect the item_sleep_log layout to the code.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemSleepLogBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    // Gets the sleep record at the current position and passes it to the ViewHolder.
    // The ViewHolder then displays the record's information.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    // ViewHolder represents one individual sleep record in the RecyclerView.
    inner class ViewHolder(
        private val binding: ItemSleepLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Displays the information from the sleep record in the item layout.
        fun bind(record: SleepLogDto) {
            val context = binding.root.context

            // Gets the user's preferred time format from their device settings.
            // This allows the app to use either 12-hour or 24-hour time.
            val timeFormat = DateFormat.getTimeFormat(context)

            // Selects the correct image based on the sleep quality rating.
            binding.imageQuality.setImageResource(
                SleepMath.faceRes(record.qualityRating)
            )

            // Converts the sleep end timestamp into a readable date.
            // The flags also display the weekday and use an abbreviated date format.
            binding.textDate.text = DateUtils.formatDateTime(
                context,
                record.sleepEnd,
                DateUtils.FORMAT_SHOW_WEEKDAY or
                        DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_ABBREV_ALL
            )

            // Converts the sleep start and end timestamps into readable times
            // and displays them together as a time range.
            binding.textTimes.text =
                "${timeFormat.format(Date(record.sleepStart))} – " +
                        timeFormat.format(Date(record.sleepEnd))

            // Converts the total duration from minutes into a more readable format,
            // such as hours and minutes.
            binding.textDuration.text =
                SleepMath.formatLong(context, record.totalDurationMinutes)

            // A normal click sends the selected record to the onClick function.
            // The screen can then use the record to open the edit screen.
            binding.root.setOnClickListener {
                onClick(record)
            }

            // A long press sends the selected record to the onLongClick function.
            // Returning true tells Android that the long-click event was handled.
            binding.root.setOnLongClickListener {
                onLongClick(record)
                true
            }
        }
    }

    // DiffUtil compares the old and new lists to find which items have changed.
    // This makes updating the RecyclerView more efficient.
    object DiffCallback : DiffUtil.ItemCallback<SleepLogDto>() {

        // Checks whether two records are the same sleep entry.
        // The sleepId is used as the unique identifier.
        override fun areItemsTheSame(
            oldItem: SleepLogDto,
            newItem: SleepLogDto
        ) = oldItem.sleepId == newItem.sleepId

        // Checks whether the contents of the two records are the same.
        // If the contents changed, the RecyclerView knows that the item needs updating.
        override fun areContentsTheSame(
            oldItem: SleepLogDto,
            newItem: SleepLogDto
        ) = oldItem == newItem
    }
}
