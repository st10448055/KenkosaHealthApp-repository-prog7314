package com.example.kenkosahealthapp_prog7314.ui.knowhealth

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.R
import kotlin.math.ceil

// Data model holding reference IDs for article content and styling resources
data class HealthArticle(
    val id: String,
    @StringRes val titleRes: Int,
    @ArrayRes val bodyRes: Int,
    @ColorRes val colorStartRes: Int,
    @ColorRes val colorEndRes: Int,
    @DrawableRes val imageRes: Int? = null
)

// Data source helper and resource utility singleton
object HealthArticles {

    // Complete list of health articles available in the app
    val all = listOf(
        HealthArticle(
            "sleep", R.string.kyh_article_sleep_title, R.array.kyh_article_sleep_body,
            R.color.kyh_card_sleep_start, R.color.kyh_card_sleep_end
        ),
        HealthArticle(
            "stress_tasks", R.string.kyh_article_stress_tasks_title, R.array.kyh_article_stress_tasks_body,
            R.color.kyh_card_stress_tasks_start, R.color.kyh_card_stress_tasks_end
        ),
        HealthArticle(
            "stretches", R.string.kyh_article_stretches_title, R.array.kyh_article_stretches_body,
            R.color.kyh_card_stretches_start, R.color.kyh_card_stretches_end
        ),
        HealthArticle(
            "eat_right", R.string.kyh_article_eat_right_title, R.array.kyh_article_eat_right_body,
            R.color.kyh_card_eat_right_start, R.color.kyh_card_eat_right_end
        ),
        HealthArticle(
            "eat_habits", R.string.kyh_article_eat_habits_title, R.array.kyh_article_eat_habits_body,
            R.color.kyh_card_eat_habits_start, R.color.kyh_card_eat_habits_end
        ),
        HealthArticle(
            "stress_methods", R.string.kyh_article_stress_methods_title, R.array.kyh_article_stress_methods_body,
            R.color.kyh_card_stress_methods_start, R.color.kyh_card_stress_methods_end
        )
    )

    // Lookup helper to retrieve an article by its unique ID
    fun byId(id: String?): HealthArticle? = all.firstOrNull { it.id == id }

    // Generates gradient drawable background using article start and end color resources
    fun gradient(context: Context, article: HealthArticle): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(
            ContextCompat.getColor(context, article.colorStartRes),
            ContextCompat.getColor(context, article.colorEndRes)
        )
    )

    // Retrieves string array containing article lines from resources
    fun body(context: Context, article: HealthArticle): Array<String> =
        context.resources.getStringArray(article.bodyRes)

    // Calculates estimated reading time in minutes (assuming ~200 words per minute)
    fun readingMinutes(context: Context, article: HealthArticle): Int {
        val words = body(context, article).sumOf { line -> line.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
        return maxOf(1, ceil(words / 200.0).toInt())
    }

    // Combines title and body text into a single lower-case string for fast search matching
    fun searchText(context: Context, article: HealthArticle): String =
        (context.getString(article.titleRes) + " " + body(context, article).joinToString(" ")).lowercase()
}
//---------------------------------------END OF FILE-------------------------------------------------