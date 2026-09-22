package com.example.kenkosahealthapp_prog7314.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CalorieRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Fraction of the ring (0f..1f) representing calories already consumed. */
    var consumedFraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var consumedColor: Int = Color.parseColor("#4A4A4A")
    var remainingColor: Int = Color.parseColor("#D4B96A")
    var strokeWidthPx: Float = 36f

    private val consumedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val remainingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        consumedPaint.color = consumedColor
        consumedPaint.strokeWidth = strokeWidthPx
        remainingPaint.color = remainingColor
        remainingPaint.strokeWidth = strokeWidthPx

        val padding = strokeWidthPx / 2f + 4f
        rect.set(padding, padding, width - padding, height - padding)

        val consumedSweep = 360f * consumedFraction
        val remainingSweep = 360f - consumedSweep

        // Remaining arc drawn first (the larger tan portion), consumed arc on top.
        canvas.drawArc(rect, -90f + consumedSweep, remainingSweep, false, remainingPaint)
        canvas.drawArc(rect, -90f, consumedSweep, false, consumedPaint)
    }
}