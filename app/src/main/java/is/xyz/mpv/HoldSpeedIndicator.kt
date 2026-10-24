package `is`.xyz.mpv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import kotlin.math.abs
import kotlin.math.max

internal class HoldSpeedIndicator(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var onCollapsed: (() -> Unit)? = null
    private val density = resources.displayMetrics.density
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 13 * resources.displayMetrics.scaledDensity
    }
    private val endpointPaint = Paint(textPaint).apply {
        textSize = 10 * resources.displayMetrics.scaledDensity
        alpha = 180
    }
    private val minimumLabel = Utils.formatSpeed(context, TouchGestures.SPEEDS.first(), minimumDecimals = 0)
    private val maximumLabel = Utils.formatSpeed(context, TouchGestures.SPEEDS.last(), minimumDecimals = 0)
    private val handler = Handler(Looper.getMainLooper())
    private var label = ""
    private var selectedIndex = 0
    private var markerPosition = 0f
    private var hintProgress = 1f
    private var markerAnimator: ValueAnimator? = null
    private var hintAnimator: ValueAnimator? = null
    private val finishFade = Runnable {
        dismiss()
        onCollapsed?.invoke()
    }
    private val fade = Runnable {
        animate().alpha(0f).setDuration(180L).start()
        handler.postDelayed(finishFade, 180L)
    }

    fun showSpeed(speed: Float, hint: Boolean, keepSpeed: Boolean = false) {
        val index = TouchGestures.SPEEDS.indexOf(speed)
        require(index >= 0) { "Unsupported hold speed: $speed" }
        val wasVisible = isVisible
        handler.removeCallbacks(fade)
        handler.removeCallbacks(finishFade)
        animate().cancel()
        alpha = 1f
        visibility = VISIBLE
        label = Utils.formatSpeed(context, speed)
        if (keepSpeed)
            label = context.getString(R.string.ui_hold_speed_keep, label)
        contentDescription = label
        selectedIndex = index
        markerAnimator?.cancel()
        if (wasVisible && !hint) {
            markerAnimator = ValueAnimator.ofFloat(markerPosition, index.toFloat()).apply {
                duration = 140L
                addUpdateListener {
                    markerPosition = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            markerPosition = index.toFloat()
        }
        hintAnimator?.cancel()
        hintProgress = 1f
        if (hint) {
            hintAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600L
                addUpdateListener {
                    hintProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        invalidate()
        requestLayout()
        if (!keepSpeed)
            handler.postDelayed(fade, 1000L)
    }

    fun dismiss() {
        handler.removeCallbacks(fade)
        handler.removeCallbacks(finishFade)
        animate().cancel()
        markerAnimator?.cancel()
        hintAnimator?.cancel()
        visibility = GONE
        alpha = 1f
    }

    override fun onDetachedFromWindow() {
        dismiss()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textHeight = textPaint.fontMetrics.run { bottom - top }
        val rowWidth = endpointPaint.measureText(minimumLabel) + endpointPaint.measureText(maximumLabel) +
            (40 + 18 * (TouchGestures.SPEEDS.size - 1)) * density
        setMeasuredDimension(
            resolveSize(kotlin.math.ceil(max(rowWidth, textPaint.measureText(label) + 16 * density)).toInt(), widthMeasureSpec),
            resolveSize((40 * density + textHeight).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val textTop = 8 * density
        canvas.drawText(label, centerX, textTop - textPaint.fontMetrics.top, textPaint)
        val y = height - 17 * density
        val minimumWidth = endpointPaint.measureText(minimumLabel)
        val maximumWidth = endpointPaint.measureText(maximumLabel)
        val spacing = minOf(18 * density,
            ((width - minimumWidth - maximumWidth - 40 * density) / (TouchGestures.SPEEDS.size - 1))
                .coerceAtLeast(0f))
        val span = spacing * (TouchGestures.SPEEDS.size - 1)
        val startX = (width - span - minimumWidth - maximumWidth - 16 * density) / 2 +
            minimumWidth + 8 * density
        val baseline = y - (endpointPaint.fontMetrics.ascent + endpointPaint.fontMetrics.descent) / 2
        endpointPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(minimumLabel, startX - 8 * density, baseline, endpointPaint)
        endpointPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(maximumLabel, startX + span + 8 * density, baseline, endpointPaint)
        for (index in TouchGestures.SPEEDS.indices) {
            val distance = abs(index - selectedIndex).toFloat() / TouchGestures.SPEEDS.lastIndex
            val pulse = if (hintProgress < 1f && index != selectedIndex)
                max(0f, 1f - abs(hintProgress - distance) * 5f)
            else 0f
            val baseAlpha = if (TouchGestures.SPEEDS[index] == 1f) 112 else 80
            dotPaint.alpha = (baseAlpha + pulse * 100).toInt()
            canvas.drawCircle(startX + index * spacing, y, (3 + pulse) * density, dotPaint)
        }
        dotPaint.alpha = 255
        canvas.drawCircle(startX + markerPosition * spacing, y, 4.5f * density, dotPaint)
    }
}
