package com.example.kenkosahealthapp_prog7314.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.data.network.GoalDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemGoalBinding

class GoalsAdapter(
    private val onGoalClick: (GoalDto) -> Unit,
    private val onGoalLongClick: (GoalDto) -> Unit
) : ListAdapter<GoalDto, GoalsAdapter.GoalViewHolder>(DiffCallback) {

    inner class GoalViewHolder(val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = getItem(position)
        holder.binding.textGoalName.text = goal.name

        val target = if (goal.targetValue == goal.targetValue.toLong().toDouble())
            goal.targetValue.toLong().toString() else goal.targetValue.toString()
        val current = if (goal.currentValue == goal.currentValue.toLong().toDouble())
            goal.currentValue.toLong().toString() else goal.currentValue.toString()

        holder.binding.textGoalProgress.text = "$current / $target"

        val percent = if (goal.targetValue > 0)
            ((goal.currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100)
        else 0
        holder.binding.progressGoal.progress = percent

        holder.binding.root.setOnClickListener { onGoalClick(goal) }
        holder.binding.root.setOnLongClickListener {
            onGoalLongClick(goal)
            true
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<GoalDto>() {
        override fun areItemsTheSame(oldItem: GoalDto, newItem: GoalDto) = oldItem.goalId == newItem.goalId
        override fun areContentsTheSame(oldItem: GoalDto, newItem: GoalDto) = oldItem == newItem
    }
}