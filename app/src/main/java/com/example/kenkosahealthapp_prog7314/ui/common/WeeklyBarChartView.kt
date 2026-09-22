package com.example.kenkosahealthapp_prog7314.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

// Custom view that draws a simple weekly bar chart (e.g. hours slept per day),
// with today's bar highlighted in a different color than the rest of the week.
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // 7 values (hours slept), index 0 = Monday .. 6 = Sunday
    private var values: List<Float> = List(7) { 0f }

    // Index of the bar to highlight (today), -1 means no highlight
    private var highlightIndex: Int = -1

    // Default color for non-highlighted bars
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7CA37F") }

    // Color used for the highlighted (today's) bar
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F3325") }

    // Called externally to update the chart with a new week of data and which day to highlight
    fun setData(weeklyHours: List<Float>, todayIndex: Int) {
        values = weeklyHours
        highlightIndex = todayIndex
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        // Scale bar heights relative to the largest value, with a minimum of 1f
        // so a chart of all-zero values doesn't divide by zero
        val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)

        // Gap between bars, sized relative to the view width
        val barCount = values.size

        // Bar width computed so barCount bars plus (barCount + 1) gaps fit exactly within the view width
        val gap = width * 0.03f
        val barWidth = (width - gap * (barCount + 1)) / barCount
        val bottom = height.toFloat()

        // Cap max bar height at 90% of view height, leaving a little breathing room at the top
        val maxBarHeight = height * 0.9f
        val cornerRadius = barWidth / 3

        for (i in values.indices) {
            // Bar height is proportional to this day's value relative to the week's max
            val barHeight = (values[i] / maxValue) * maxBarHeight
            val left = gap + i * (barWidth + gap)
            val top = bottom - barHeight

            // Use the highlight color only for today's bar, default color for the rest
            val paint = if (i == highlightIndex) highlightPaint else barPaint
            canvas.drawRoundRect(RectF(left, top, left + barWidth, bottom), cornerRadius, cornerRadius, paint)
        }
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------