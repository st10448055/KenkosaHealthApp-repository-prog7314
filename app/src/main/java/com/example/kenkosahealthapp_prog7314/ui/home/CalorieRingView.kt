package com.example.kenkosahealthapp_prog7314.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

// Custom View that renders a circular ring showing consumed vs remaining calories
class CalorieRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Fraction of the ring (0f..1f) representing calories already consumed
    var consumedFraction: Float = 0f
        set(value) {
            // Constrain value between 0.0 and 1.0 and trigger a redraw
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // Color for the consumed calories arc segment
    var consumedColor: Int = Color.parseColor("#4A4A4A")

    // Color for the remaining calories arc segment
    var remainingColor: Int = Color.parseColor("#D4B96A")

    // Thickness of the ring stroke in pixels
    var strokeWidthPx: Float = 36f

    // Paint configuration for drawing consumed calories
    private val consumedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Paint configuration for drawing remaining calories
    private val remainingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Bounding rectangle defining the boundaries of the circular arcs
    private val rect = RectF()

    // Handles view drawing logic on canvas
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Set paint colors and stroke widths
        consumedPaint.color = consumedColor
        consumedPaint.strokeWidth = strokeWidthPx
        remainingPaint.color = remainingColor
        remainingPaint.strokeWidth = strokeWidthPx

        // Calculate padding to prevent stroke clipping on view borders
        val padding = strokeWidthPx / 2f + 4f
        rect.set(padding, padding, width - padding, height - padding)

        // Calculate sweep angles for both progress segments
        val consumedSweep = 360f * consumedFraction
        val remainingSweep = 360f - consumedSweep

        // Draw remaining arc first (starting where consumed ends), then consumed arc starting from top (-90 degrees)
        canvas.drawArc(rect, -90f + consumedSweep, remainingSweep, false, remainingPaint)
        canvas.drawArc(rect, -90f, consumedSweep, false, consumedPaint)
    }
}
//---------------------------------------END OF FILE-------------------------------------------------