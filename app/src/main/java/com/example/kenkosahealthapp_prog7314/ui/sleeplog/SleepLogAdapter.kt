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

/** "Recent Sleep" list. Tap = edit, long-press = delete. */
class SleepLogAdapter(
    private val onClick: (SleepLogDto) -> Unit,
    private val onLongClick: (SleepLogDto) -> Unit
) : ListAdapter<SleepLogDto, SleepLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemSleepLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemSleepLogBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SleepLogDto) {
            val context = binding.root.context
            val timeFormat = DateFormat.getTimeFormat(context)

            binding.imageQuality.setImageResource(SleepMath.faceRes(record.qualityRating))
            binding.textDate.text = DateUtils.formatDateTime(
                context, record.sleepEnd,
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
            )
            binding.textTimes.text = "${timeFormat.format(Date(record.sleepStart))} – ${timeFormat.format(Date(record.sleepEnd))}"
            binding.textDuration.text = SleepMath.formatLong(context, record.totalDurationMinutes)

            binding.root.setOnClickListener { onClick(record) }
            binding.root.setOnLongClickListener {
                onLongClick(record)
                true
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<SleepLogDto>() {
        override fun areItemsTheSame(oldItem: SleepLogDto, newItem: SleepLogDto) = oldItem.sleepId == newItem.sleepId
        override fun areContentsTheSame(oldItem: SleepLogDto, newItem: SleepLogDto) = oldItem == newItem
    }
}
