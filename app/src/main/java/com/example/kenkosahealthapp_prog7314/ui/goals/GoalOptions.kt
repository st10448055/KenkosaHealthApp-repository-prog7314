package com.example.kenkosahealthapp_prog7314.ui.goals

import androidx.annotation.StringRes
import com.example.kenkosahealthapp_prog7314.R

// Available goal category options mapped to default string resource units
enum class GoalCategoryOption(@StringRes val defaultUnitRes: Int?) {
    STEPS(R.string.goaltrack_unit_steps),
    WATER(R.string.goaltrack_unit_water),
    SLEEP(R.string.goaltrack_unit_sleep),
    EXERCISE(R.string.goaltrack_unit_exercise),
    CUSTOM(null);

    companion object {
        // Returns corresponding enum entry for string key or defaults to CUSTOM
        fun fromKey(key: String?): GoalCategoryOption = values().firstOrNull { it.name == key } ?: CUSTOM
    }
}

// Maps frequency string constant to localized string resource ID
fun frequencyLabelRes(frequency: String): Int = when (frequency) {
    GoalProgress.WEEKLY -> R.string.goaltrack_freq_weekly
    GoalProgress.MONTHLY -> R.string.goaltrack_freq_monthly
    else -> R.string.goaltrack_freq_daily
}
//---------------------------------------END OF FILE-------------------------------------------------