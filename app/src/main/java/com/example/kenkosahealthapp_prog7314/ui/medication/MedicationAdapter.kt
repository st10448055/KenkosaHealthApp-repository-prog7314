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

/** What one card shows. Built by the fragment so "taken today" and the time label are already resolved. */
data class MedicationRow(
    val medication: MedicationDto,
    val takenToday: Boolean,
    val timeLabel: String
)

/**
 * Used for both lists on the Medication screen. Vitamins get the small grey card,
 * everything else gets the large green "pill" card with the tick circle.
 */
class MedicationAdapter(
    private val onTakenToggle: (MedicationRow) -> Unit,
    private val onClick: (MedicationDto) -> Unit,
    private val onLongClick: (MedicationDto) -> Unit
) : ListAdapter<MedicationRow, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).medication.category == MedicationSchedule.CATEGORY_VITAMIN) TYPE_VITAMIN
        else TYPE_PILL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_VITAMIN) {
            VitaminViewHolder(ItemMedicationVitaminBinding.inflate(inflater, parent, false))
        } else {
            PillViewHolder(ItemMedicationPillBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = getItem(position)
        when (holder) {
            is PillViewHolder -> holder.bind(row)
            is VitaminViewHolder -> holder.bind(row)
        }
    }

    private fun RecyclerView.ViewHolder.wireClicks(row: MedicationRow) {
        itemView.setOnClickListener { onClick(row.medication) }
        itemView.setOnLongClickListener {
            onLongClick(row.medication)
            true
        }
    }

    inner class PillViewHolder(private val binding: ItemMedicationPillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: MedicationRow) {
            binding.textName.text = row.medication.name
            binding.textDosage.text = row.medication.dosage
            binding.textTime.text = row.timeLabel

            binding.buttonTaken.setImageResource(
                if (row.takenToday) R.drawable.ic_check_ring_on else R.drawable.ic_check_ring_off
            )
            binding.buttonTaken.contentDescription = binding.root.context.getString(
                if (row.takenToday) R.string.medication_mark_not_taken else R.string.medication_mark_taken
            )
            binding.buttonTaken.setOnClickListener { onTakenToggle(row) }
            binding.root.alpha = if (row.takenToday) 0.75f else 1f

            wireClicks(row)
        }
    }

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

    object DiffCallback : DiffUtil.ItemCallback<MedicationRow>() {
        override fun areItemsTheSame(oldItem: MedicationRow, newItem: MedicationRow) =
            oldItem.medication.medicationId == newItem.medication.medicationId

        override fun areContentsTheSame(oldItem: MedicationRow, newItem: MedicationRow) =
            oldItem == newItem
    }

    private companion object {
        const val TYPE_PILL = 0
        const val TYPE_VITAMIN = 1
    }
}
