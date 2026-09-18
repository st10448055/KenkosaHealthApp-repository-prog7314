package com.example.kenkosahealthapp_prog7314.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // 7 values (hours slept), index 0 = Monday .. 6 = Sunday
    private var values: List<Float> = List(7) { 0f }
    private var highlightIndex: Int = -1

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7CA37F") }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F3325") }

    fun setData(weeklyHours: List<Float>, todayIndex: Int) {
        values = weeklyHours
        highlightIndex = todayIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val barCount = values.size
        val gap = width * 0.03f
        val barWidth = (width - gap * (barCount + 1)) / barCount
        val bottom = height.toFloat()
        val maxBarHeight = height * 0.9f
        val cornerRadius = barWidth / 3

        for (i in values.indices) {
            val barHeight = (values[i] / maxValue) * maxBarHeight
            val left = gap + i * (barWidth + gap)
            val top = bottom - barHeight
            val paint = if (i == highlightIndex) highlightPaint else barPaint
            canvas.drawRoundRect(RectF(left, top, left + barWidth, bottom), cornerRadius, cornerRadius, paint)
        }
    }
}