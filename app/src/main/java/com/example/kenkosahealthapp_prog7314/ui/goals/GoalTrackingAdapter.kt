package com.example.kenkosahealthapp_prog7314.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.TrackedGoalDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemGoalTrackingBinding

// Row wrapper combining goal entity with pre-computed progress and percentage
data class GoalRow(
    val goal: TrackedGoalDto,
    val periodValue: Double,
    val percent: Int
)

// ListAdapter for displaying goal cards in a RecyclerView
class GoalTrackingAdapter(
    private val onClick: (TrackedGoalDto) -> Unit
) : ListAdapter<GoalRow, GoalTrackingAdapter.ViewHolder>(DiffCallback) {

    // Inflates item layout binding and creates ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemGoalTrackingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    // Binds GoalRow data to ViewHolder item views
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    // ViewHolder managing bindings and user click events for goal items
    inner class ViewHolder(private val binding: ItemGoalTrackingBinding) : RecyclerView.ViewHolder(binding.root) {

        // Binds data properties to UI views
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

            // Register tap and long press interactions
            binding.root.setOnClickListener { onClick(goal) }
            binding.root.setOnLongClickListener {
                onClick(goal)
                true
            }
        }
    }

    // Item callback calculating differences between goal rows for animations
    object DiffCallback : DiffUtil.ItemCallback<GoalRow>() {
        override fun areItemsTheSame(oldItem: GoalRow, newItem: GoalRow) = oldItem.goal.goalId == newItem.goal.goalId
        override fun areContentsTheSame(oldItem: GoalRow, newItem: GoalRow) = oldItem == newItem
    }
}
//---------------------------------------END OF FILE-------------------------------------------------