package com.example.kenkosahealthapp_prog7314.ui.fasting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.R

// Custom View that draws a circular progress ring representing how far along
// the current fast is (0f = just started, 1f = goal reached / full circle).
class FastingRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Current progress fraction (0..1). Setting it clamps the value and
    // triggers a redraw so the arc updates on screen.
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // Ring thickness in pixels, scaled for screen density (22dp).
    private val strokePx = 22f * resources.displayMetrics.density

    // Paint for the background "track" (the full, unfilled circle).
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
        color = ContextCompat.getColor(context, R.color.fast_ring_track)
    }

    // Paint for the foreground progress arc (the portion representing elapsed time).
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
        color = ContextCompat.getColor(context, R.color.fast_ring_progress)
    }

    // Reused bounding box for the arcs, recalculated on every draw to account
    // for the view's current size/padding (avoids allocating a new RectF each frame).
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Inset the bounding box by half the stroke width so the stroke doesn't
        // get clipped at the view's edges.
        val half = strokePx / 2f
        oval.set(
            paddingLeft + half,
            paddingTop + half,
            width - paddingRight - half,
            height - paddingBottom - half
        )

        // Draw the full background track first (a complete 360° circle).
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        // and sweeping clockwise proportionally to `progress`.
        if (progress > 0f) {
            canvas.drawArc(oval, -90f, 360f * progress, false, progressPaint)
        }
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------