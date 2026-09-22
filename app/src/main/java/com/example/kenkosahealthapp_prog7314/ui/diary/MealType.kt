package com.example.kenkosahealthapp_prog7314.ui.diary

import com.example.kenkosahealthapp_prog7314.R
import androidx.annotation.StringRes

enum class MealType(val apiValue: String, @StringRes val labelRes: Int) {
    BREAKFAST("breakfast", R.string.mealtype_breakfast),
    LUNCH("lunch", R.string.mealtype_lunch),
    DINNER("dinner", R.string.mealtype_dinner),
    SNACK("snack", R.string.mealtype_snack)
}