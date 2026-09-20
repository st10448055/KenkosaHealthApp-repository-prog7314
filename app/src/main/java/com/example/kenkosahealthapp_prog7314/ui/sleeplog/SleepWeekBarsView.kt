package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.R

/**
 * "Weekly Patterns": seven bars, Monday to Sunday, each as tall as that day's sleep. The selected day is dark;
 * tap a bar to select another day.
 */
class SleepWeekBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onBarSelected: ((Int) -> Unit)? = null

    private var minutes: List<Int> = List(DAYS) { 0 }
    private var selectedIndex = -1

    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sleeplog_bar)
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sleeplog_bar_selected)
    }
    private val rect = RectF()

    fun setData(minutes: List<Int>, selectedIndex: Int) {
        this.minutes = if (minutes.size == DAYS) minutes else List(DAYS) { 0 }
        this.selectedIndex = selectedIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val slot = width / DAYS.toFloat()
        val barWidth = slot * 0.62f
        val radius = 8f * density
        val stub = 6f * density
        val usable = height.toFloat()

        // Scale to at least a 10 hour night so a short week doesn't look full.
        val scale = maxOf(MIN_SCALE_MINUTES, minutes.maxOrNull() ?: 0).toFloat()

        for (i in 0 until DAYS) {
            val value = minutes[i]
            val barHeight = if (value <= 0) stub else maxOf(stub, value / scale * usable)
            val left = i * slot + (slot - barWidth) / 2f
            rect.set(left, height - barHeight, left + barWidth, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, if (i == selectedIndex) selectedPaint else barPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val slot = width / DAYS.toFloat()
                val index = (event.x / slot).toInt().coerceIn(0, DAYS - 1)
                onBarSelected?.invoke(index)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private companion object {
        const val DAYS = 7
        const val MIN_SCALE_MINUTES = 10 * 60
    }
}
