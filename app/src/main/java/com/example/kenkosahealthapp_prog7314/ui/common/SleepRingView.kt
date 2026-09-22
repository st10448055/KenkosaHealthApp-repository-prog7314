package com.example.kenkosahealthapp_prog7314.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

// Custom view that draws a circular progress ring (used to show sleep progress
// toward a goal, e.g. "6 of 8 hours"). Draws a full grey background circle,
// then an arc on top representing the current progress fraction.
class SleepRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress: Float = 0f // 0f..1f

    // Full background ring, always drawn at 360 degrees
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        color = Color.parseColor("#E4EAE1")
        strokeCap = Paint.Cap.ROUND
    }

    // Foreground arc representing actual progress, drawn on top of the background ring
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        color = Color.parseColor("#1F3325")
        strokeCap = Paint.Cap.ROUND
    }

    // Reused bounding box for the arcs, avoids allocating a new RectF on every draw call
    private val rect = RectF()

    // Called externally to update how much of the ring should be filled (0..1)
    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Inset the bounding box by half the stroke width so the stroke doesn't get clipped at the edges
        val strokeHalf = backgroundPaint.strokeWidth / 2
        rect.set(strokeHalf, strokeHalf, width - strokeHalf, height - strokeHalf)
        // Draw the full background ring (starts at -90 degrees, i.e. 12 o'clock, going the full 360)
        canvas.drawArc(rect, -90f, 360f, false, backgroundPaint)
        // Draw the progress arc on top, only covering the completed fraction
        canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------