package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.ItemMeditationVideoBinding

class MeditationVideoAdapter(
    private val videos: List<MeditationVideo>,
    private val onClick: (MeditationVideo) -> Unit
) : RecyclerView.Adapter<MeditationVideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemMeditationVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(videos[position])

    override fun getItemCount(): Int = videos.size

    inner class ViewHolder(private val binding: ItemMeditationVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: MeditationVideo) {
            val context = binding.root.context
            binding.textTitle.text = video.title
            binding.textDuration.text = context.getString(R.string.meditation_minutes, video.minutes)
            binding.imageThumb.load(video.thumbnailUrl) { crossfade(true) }
            binding.root.contentDescription = context.getString(R.string.meditation_play, video.title)
            binding.root.setOnClickListener { onClick(video) }
        }
    }
}
