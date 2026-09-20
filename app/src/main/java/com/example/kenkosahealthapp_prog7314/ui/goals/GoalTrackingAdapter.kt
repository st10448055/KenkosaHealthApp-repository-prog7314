package com.example.kenkosahealthapp_prog7314.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemGoalTrackingBinding

/** A goal plus its progress for the current period, worked out by the fragment for today's date. */
data class GoalRow(
    val goal: TrackedGoalDto,
    val periodValue: Double,
    val percent: Int
)

/** Goal cards. Tap (or long-press) opens the Log progress / Edit / Delete menu. */
class GoalTrackingAdapter(
    private val onClick: (TrackedGoalDto) -> Unit
) : ListAdapter<GoalRow, GoalTrackingAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemGoalTrackingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemGoalTrackingBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: GoalRow) {
            val context = binding.root.context
            val goal = row.goal

            binding.textGoalName.text = goal.name
            binding.textGoalProgress.text = context.getString(
                R.string.goaltrack_progress,
                GoalProgress.formatCompact(row.periodValue),
                GoalProgress.formatCompact(goal.targetValue)
            )
            binding.progressGoal.setProgressCompat(row.percent, false)
            binding.textGoalMeta.text = context.getString(
                R.string.goaltrack_meta,
                context.getString(frequencyLabelRes(goal.frequency)),
                goal.unit
            )

            binding.root.setOnClickListener { onClick(goal) }
            binding.root.setOnLongClickListener {
                onClick(goal)
                true
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<GoalRow>() {
        override fun areItemsTheSame(oldItem: GoalRow, newItem: GoalRow) = oldItem.goal.goalId == newItem.goal.goalId
        override fun areContentsTheSame(oldItem: GoalRow, newItem: GoalRow) = oldItem == newItem
    }
}
