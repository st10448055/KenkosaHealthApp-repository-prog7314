package com.example.kenkosahealthapp_prog7314.ui.sleeplog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.R

// Custom View used to draw the sleep progress ring on the Sleep screen.
// The background circle represents the full target, while the progress arc
// shows how much of the target has been reached.
class SleepRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Stores the current progress of the sleep ring.
    // 0 means no progress and 1 means the full sleep target has been reached.
    var progress: Float = 0f
        set(value) {
            // Prevents the progress from going below 0 or above 1.
            field = value.coerceIn(0f, 1f)

            // Tells Android that the View needs to be drawn again
            // because the progress value has changed.
            invalidate()
        }

    // Sets the thickness of the ring.
    // The density value converts the size into pixels so it scales correctly
    // across devices with different screen densities.
    private val strokePx =
        18f * resources.displayMetrics.density

    // Paint used to draw the light background track of the ring.
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        // Draws only the outline of the circle instead of filling it.
        style = Paint.Style.STROKE

        // Sets the thickness of the ring.
        strokeWidth = strokePx

        // Gets the track colour from the app's colour resources.
        color = ContextCompat.getColor(
            context,
            R.color.sleeplog_ring_track
        )
    }

    // Paint used to draw the coloured progress section of the ring.
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        // Draws the progress as an outline around the circle.
        style = Paint.Style.STROKE

        // Makes the ends of the progress arc rounded.
        strokeCap = Paint.Cap.ROUND

        // Uses the same thickness as the background track.
        strokeWidth = strokePx

        // Gets the progress colour from the app's resources.
        color = ContextCompat.getColor(
            context,
            R.color.sleeplog_ring_progress
        )
    }

    // Defines the rectangular area in which the circle/arc will be drawn.
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Half the stroke width is used as an offset so that the thick ring
        // stays inside the View's available drawing area.
        val half = strokePx / 2f

        // Sets the boundaries of the circle based on the View's size and padding.
        oval.set(
            paddingLeft + half,
            paddingTop + half,
            width - paddingRight - half,
            height - paddingBottom - half
        )

        // Draws the complete background circle.
        // This represents the full sleep target.
        canvas.drawArc(
            oval,
            0f,
            360f,
            false,
            trackPaint
        )

        // Only draw the progress section if there is some sleep progress.
        if (progress > 0f) {

            // Starts at -90 degrees so that the progress begins at the top
            // of the circle instead of the right side.
            //
            // 360 degrees represents a complete circle, so multiplying
            // 360 by progress determines how much of the circle to draw.
            canvas.drawArc(
                oval,
                -90f,
                360f * progress,
                false,
                progressPaint
            )
        }
    }
}
