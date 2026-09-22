package com.example.kenkosahealthapp_prog7314.ui.medication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.network.MedicationDto
import com.example.kenkosahealthapp_prog7314.databinding.ItemMedicationPillBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemMedicationVitaminBinding

// Represents one medication item that will be displayed in the RecyclerView.
// It combines the medication information with information calculated for the screen,
// such as whether it has already been taken today and the formatted reminder time.
data class MedicationRow(
    val medication: MedicationDto,
    val takenToday: Boolean,
    val timeLabel: String
)

// Adapter used to display medication items in the RecyclerViews.
// It supports two different layouts:
// 1. A larger pill/medication card.
// 2. A smaller vitamin card.
//
// The adapter also handles clicks, long-clicks and marking medication as taken.
class MedicationAdapter(
    private val onTakenToggle: (MedicationRow) -> Unit,
    private val onClick: (MedicationDto) -> Unit,
    private val onLongClick: (MedicationDto) -> Unit
) : ListAdapter<MedicationRow, RecyclerView.ViewHolder>(DiffCallback) {

    // Determines which layout should be used for each medication.
    // Vitamins use the vitamin layout while normal medication uses the pill layout.
    override fun getItemViewType(position: Int): Int =
        if (getItem(position).medication.category == MedicationSchedule.CATEGORY_VITAMIN) TYPE_VITAMIN
        else TYPE_PILL

    // Creates the correct ViewHolder based on the item type.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_VITAMIN) {
            VitaminViewHolder(ItemMedicationVitaminBinding.inflate(inflater, parent, false))
        } else {
            PillViewHolder(ItemMedicationPillBinding.inflate(inflater, parent, false))
        }
    }

    // Connects the medication data at the specified position to its ViewHolder.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Gets the medication row that belongs to this position.
        val row = getItem(position)
        // Calls the correct bind function depending on the ViewHolder type.
        when (holder) {
            is PillViewHolder -> holder.bind(row)
            is VitaminViewHolder -> holder.bind(row)
        }
    }

    // Adds the normal click and long-click behaviour to a medication item.
    private fun RecyclerView.ViewHolder.wireClicks(row: MedicationRow) {
        // Adds the normal click and long-click behaviour to a medication item.
        itemView.setOnClickListener { onClick(row.medication) }

        // A long click opens the delete confirmation.
        // Returning true tells Android that the long-click event was handled.
        itemView.setOnLongClickListener {
            onLongClick(row.medication)
            true
        }
    }

    inner class PillViewHolder(private val binding: ItemMedicationPillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Places the medication information into the pill card.
        fun bind(row: MedicationRow) {
            // Display the medication name.
            binding.textName.text = row.medication.name
            // Display the dosage.
            binding.textDosage.text = row.medication.dosage
            // Display the formatted reminder time.
            binding.textTime.text = row.timeLabel

            // Change the tick icon depending on whether the medication was taken today.
            binding.buttonTaken.setImageResource(
                if (row.takenToday) R.drawable.ic_check_ring_on else R.drawable.ic_check_ring_off
            )

            // Provides an accessibility description for screen readers.
            // The text changes depending on whether the medication is currently marked as taken.
            binding.buttonTaken.contentDescription = binding.root.context.getString(
                if (row.takenToday) R.string.medication_mark_not_taken else R.string.medication_mark_taken
            )
            binding.buttonTaken.setOnClickListener { onTakenToggle(row) }
            binding.root.alpha = if (row.takenToday) 0.75f else 1f

            wireClicks(row)
        }
    }

    // ViewHolder used for vitamin items.
    inner class VitaminViewHolder(private val binding: ItemMedicationVitaminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: MedicationRow) {
            binding.textName.text = row.medication.name
            binding.textSubtitle.text = binding.root.context.getString(
                R.string.medication_vitamin_subtitle, row.medication.dosage, row.timeLabel
            )
            wireClicks(row)
        }
    }

    // DiffUtil determines which items changed when a new list is submitted.
    // This prevents the RecyclerView from unnecessarily redrawing every item.
    object DiffCallback : DiffUtil.ItemCallback<MedicationRow>() {

        // Determines whether two rows represent the same medication.
        // The medication ID is used as the unique identifier.
        override fun areItemsTheSame(oldItem: MedicationRow, newItem: MedicationRow) =
            oldItem.medication.medicationId == newItem.medication.medicationId

        // Determines whether the contents of the medication have changed.
        // Because MedicationRow is a data class, == compares all of its properties.
        override fun areContentsTheSame(oldItem: MedicationRow, newItem: MedicationRow) =
            oldItem == newItem
    }

    // Constants used to identify the two different RecyclerView item types.
    private companion object {
        const val TYPE_PILL = 0
        const val TYPE_VITAMIN = 1
    }
}
//---------------------------------------END OF FILE-------------------------------------------------