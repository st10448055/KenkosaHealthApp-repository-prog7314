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
 * "Weekly Patterns": seven bars, Monday to Sunday, each as tall as that day's sleep.
 * The selected day is dark and tapping a bar selects that day.
 */
class SleepWeekBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Sends the selected bar index back to the Fragment when a user taps a day.
    var onBarSelected: ((Int) -> Unit)? = null

    // Stores the total sleep minutes for each day of the week.
    private var minutes: List<Int> = List(DAYS) { 0 }

    // Keeps track of which day is currently selected.
    private var selectedIndex = -1

    // Used to convert sizes from dp-style values into actual screen pixels.
    private val density = resources.displayMetrics.density

    // Paint used for the normal, unselected bars.
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sleeplog_bar)
    }

    // Paint used to make the selected day's bar visually different.
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.sleeplog_bar_selected)
    }

    // Reusable rectangle that defines the position and size of each bar.
    private val rect = RectF()

    // Updates the chart with new sleep data and redraws the view.
    fun setData(minutes: List<Int>, selectedIndex: Int) {
        // Make sure there are exactly seven values, one for each day.
        this.minutes = if (minutes.size == DAYS) minutes else List(DAYS) { 0 }
        this.selectedIndex = selectedIndex

        // Redraw the bars using the updated data.
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Divide the view into seven equal horizontal sections.
        val slot = width / DAYS.toFloat()

        // Each bar uses 62% of its available section, leaving space between bars.
        val barWidth = slot * 0.62f

        // Controls how rounded the corners of the bars are.
        val radius = 8f * density

        // Gives days with no recorded sleep a small visible bar.
        val stub = 6f * density

        // The full height available for drawing the bars.
        val usable = height.toFloat()

        // Scale the bars to at least 10 hours so a short week does not appear full.
        val scale = maxOf(MIN_SCALE_MINUTES, minutes.maxOrNull() ?: 0).toFloat()

        // Draw one bar for each day from Monday to Sunday.
        for (i in 0 until DAYS) {
            val value = minutes[i]

            // Convert the sleep minutes into a proportional bar height.
            val barHeight = if (value <= 0) stub else maxOf(stub, value / scale * usable)

            // Centre the bar inside its seven-day section.
            val left = i * slot + (slot - barWidth) / 2f

            // Set the rectangle's position from the calculated height.
            rect.set(left, height - barHeight, left + barWidth, height.toFloat())

            // Use the selected colour for the active day and the normal colour for others.
            canvas.drawRoundRect(
                rect,
                radius,
                radius,
                if (i == selectedIndex) selectedPaint else barPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            // Tell Android that this view wants to receive the rest of the touch event.
            MotionEvent.ACTION_DOWN -> return true

            MotionEvent.ACTION_UP -> {
                // Work out which of the seven sections the user tapped.
                val slot = width / DAYS.toFloat()
                val index = (event.x / slot).toInt().coerceIn(0, DAYS - 1)

                // Notify the Fragment which day was selected.
                onBarSelected?.invoke(index)

                // Treat the touch as a click for normal Android click handling.
                performClick()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    // Keeps accessibility and standard Android click behaviour working correctly.
    override fun performClick(): Boolean = super.performClick()

    private companion object {
        // The chart always represents seven days, Monday through Sunday.
        const val DAYS = 7

        // Sets the minimum scale to 10 hours of sleep.
        const val MIN_SCALE_MINUTES = 10 * 60
    }
}