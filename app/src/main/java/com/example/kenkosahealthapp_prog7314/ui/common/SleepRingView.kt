package com.example.kenkosahealthapp_prog7314.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SleepRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress: Float = 0f // 0f..1f
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        color = Color.parseColor("#E4EAE1")
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        color = Color.parseColor("#1F3325")
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strokeHalf = backgroundPaint.strokeWidth / 2
        rect.set(strokeHalf, strokeHalf, width - strokeHalf, height - strokeHalf)
        canvas.drawArc(rect, -90f, 360f, false, backgroundPaint)
        canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
    }
}