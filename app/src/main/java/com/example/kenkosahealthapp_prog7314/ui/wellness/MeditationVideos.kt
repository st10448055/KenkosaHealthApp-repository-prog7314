package com.example.kenkosahealthapp_prog7314.ui.wellness

/**
 * Represents one YouTube meditation video used by the Wellness screen.
 * The YouTube ID and thumbnail URL are calculated from the normal YouTube URL.
 */
data class MeditationVideo(
    val title: String,
    val url: String,
    val minutes: Int
) {

    // Extract the unique 11-character YouTube video ID from the URL.
    // This supports standard YouTube links, youtu.be, Shorts, embed and live links.
    val videoId: String?
        get() = YOUTUBE_ID.find(url)?.groupValues?.get(1)

    // Use the extracted video ID to create a URL for the video's thumbnail.
    // If the ID cannot be found, the thumbnail URL will be null.
    val thumbnailUrl: String?
        get() = videoId?.let {
            "https://img.youtube.com/vi/$it/hqdefault.jpg"
        }

    private companion object {

        // Regular expression used to find the YouTube video ID in different URL formats.
        val YOUTUBE_ID = Regex(
            "(?:youtu\\.be/|v=|/embed/|/shorts/|/live/)([A-Za-z0-9_-]{11})"
        )
    }
}

/**
 * Stores the meditation videos displayed on the Wellness screen.
 * Videos can be added, removed or reordered in this list.
 */
object MeditationVideos {

    // Central list of meditation content used by the app.
    val all = listOf(

        MeditationVideo(
            title = "10-Minute Guided Meditation for Complete Beginners",
            url = "https://www.youtube.com/watch?v=S-W1GFBJbt0",
            minutes = 10
        ),

        MeditationVideo(
            title = "10-Minute Meditation to Start Your Day",
            url = "https://www.youtube.com/watch?v=ENYYb5vIMkU",
            minutes = 10
        ),

        MeditationVideo(
            title = "10-Minute Guided Meditation to Clear Your Thoughts",
            url = "https://www.youtube.com/watch?v=WHCl6b9K25Y",
            minutes = 10
        ),

        MeditationVideo(
            title = "5-Minute Guided Breathing Meditation for Anxiety",
            url = "https://www.youtube.com/watch?v=nJYwx94tLdA",
            minutes = 5
        ),

        MeditationVideo(
            title = "5-Minute Deep Breathing Mindful Relaxation",
            url = "https://www.youtube.com/watch?v=he-tQOnDCWw",
            minutes = 5
        ),

        MeditationVideo(
            title = "5-Minute Calming Breathwork",
            url = "https://www.youtube.com/watch?v=9fEo9my03Ks",
            minutes = 5
        )
    )
}