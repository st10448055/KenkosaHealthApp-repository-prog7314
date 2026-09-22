package com.example.kenkosahealthapp_prog7314.ui.goals

import androidx.annotation.StringRes
import com.example.kenkosahealthapp_prog7314.R

/** The category chips. The enum name is what's stored (and what the Sleep screen looks for: SLEEP). */
enum class GoalCategoryOption(@StringRes val defaultUnitRes: Int?) {
    STEPS(R.string.goaltrack_unit_steps),
    WATER(R.string.goaltrack_unit_water),
    SLEEP(R.string.goaltrack_unit_sleep),
    EXERCISE(R.string.goaltrack_unit_exercise),
    CUSTOM(null);

    companion object {
        fun fromKey(key: String?): GoalCategoryOption = values().firstOrNull { it.name == key } ?: CUSTOM
    }
}

fun frequencyLabelRes(frequency: String): Int = when (frequency) {
    GoalProgress.WEEKLY -> R.string.goaltrack_freq_weekly
    GoalProgress.MONTHLY -> R.string.goaltrack_freq_monthly
    else -> R.string.goaltrack_freq_daily
}
