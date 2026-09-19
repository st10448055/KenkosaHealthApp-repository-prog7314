package com.example.kenkosahealthapp_prog7314.ui.wellness

/**
 * One YouTube meditation video. Paste any normal YouTube link into [url]
 * (watch?v=..., youtu.be/..., /shorts/..., /embed/... all work); the thumbnail is worked out from it.
 */
data class MeditationVideo(
    val title: String,
    val url: String,
    val minutes: Int
) {
    val videoId: String? get() = YOUTUBE_ID.find(url)?.groupValues?.get(1)

    val thumbnailUrl: String? get() = videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

    private companion object {
        val YOUTUBE_ID = Regex("(?:youtu\\.be/|v=|/embed/|/shorts/|/live/)([A-Za-z0-9_-]{11})")
    }
}

/** The list shown on the Wellness screen. Add, remove or reorder entries here. */
object MeditationVideos {
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
