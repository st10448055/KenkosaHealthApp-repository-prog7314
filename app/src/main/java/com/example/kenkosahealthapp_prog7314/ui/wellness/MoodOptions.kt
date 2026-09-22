package com.example.kenkosahealthapp_prog7314.ui.wellness

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.kenkosahealthapp_prog7314.R

// Represents the five mood options available to the user.
// The enum name is also used as the value stored by the backend.
enum class Mood(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
) {
    VERY_HAPPY(R.drawable.ic_mood_very_happy, R.string.mood_very_happy),
    HAPPY(R.drawable.ic_mood_happy, R.string.mood_happy),
    NEUTRAL(R.drawable.ic_mood_neutral, R.string.mood_neutral),
    SAD(R.drawable.ic_mood_sad, R.string.mood_sad),
    VERY_SAD(R.drawable.ic_mood_very_sad, R.string.mood_very_sad);

    companion object {

        // Convert a stored backend key back into the matching Mood.
        // Returns null if the key does not match any available mood.
        fun fromKey(key: String?): Mood? =
            values().firstOrNull { it.name == key }
    }
}

// Represents the "Feeling" chips that can be selected alongside a mood.
// To add a new feeling, it needs to be added here, to the backend enum,
// and to the corresponding string resources.
enum class Feeling(
    @StringRes val labelRes: Int,
    @ColorRes val colorRes: Int
) {
    CALM(R.string.feeling_calm, R.color.feeling_calm),
    CONTENT(R.string.feeling_content, R.color.feeling_content),
    EXCITED(R.string.feeling_excited, R.color.feeling_excited),
    UPSET(R.string.feeling_upset, R.color.feeling_upset),
    IRRITATED(R.string.feeling_irritated, R.color.feeling_irritated),
    HAPPY(R.string.feeling_happy, R.color.feeling_happy),
    MOTIVATED(R.string.feeling_motivated, R.color.feeling_motivated),
    GRATEFUL(R.string.feeling_grateful, R.color.feeling_grateful),
    PROUD(R.string.feeling_proud, R.color.feeling_proud);

    companion object {

        // Convert a stored backend key into the matching Feeling.
        // Returns null when the key is not recognised.
        fun fromKey(key: String?): Feeling? =
            values().firstOrNull { it.name == key }
    }
}