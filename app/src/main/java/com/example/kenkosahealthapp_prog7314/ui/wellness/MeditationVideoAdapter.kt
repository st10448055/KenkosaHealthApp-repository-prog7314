package com.example.kenkosahealthapp_prog7314.ui.wellness

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.ItemMeditationVideoBinding

class MeditationVideoAdapter(
    // List of meditation videos that will be displayed in the RecyclerView.
    private val videos: List<MeditationVideo>,

    // Function called when the user selects a video.
    private val onClick: (MeditationVideo) -> Unit
) : RecyclerView.Adapter<MeditationVideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        // Inflate the layout for one meditation video item.
        ViewHolder(
            ItemMeditationVideoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        // Bind the video at this position to the ViewHolder.
        holder.bind(videos[position])

    override fun getItemCount(): Int =
        // Tell the RecyclerView how many video items need to be displayed.
        videos.size

    // ViewHolder represents one item in the meditation video list.
    inner class ViewHolder(
        private val binding: ItemMeditationVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: MeditationVideo) {
            val context = binding.root.context

            // Display the video's title and duration.
            binding.textTitle.text = video.title
            binding.textDuration.text =
                context.getString(R.string.meditation_minutes, video.minutes)

            // Coil loads the thumbnail image from its URL.
            // Crossfade makes the image appear more smoothly once it has loaded.
            binding.imageThumb.load(video.thumbnailUrl) {
                crossfade(true)
            }

            // Provide a description for accessibility tools such as screen readers.
            binding.root.contentDescription =
                context.getString(R.string.meditation_play, video.title)

            // Open the selected meditation video when the item is tapped.
            binding.root.setOnClickListener {
                onClick(video)
            }
        }
    }
}