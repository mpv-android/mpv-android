package `is`.xyz.mpv

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.*

enum class PropertyChange {
    Init,
    Seek,
    Volume,
    Bright,
    Finalize,
    SpeedInit,
    Speed,
    SpeedKeep,
    SpeedCommit,
    SpeedFinalize,

    /* Tap gestures */
    SeekFixed,
    PlayPause,
    Custom,
}

internal interface TouchGesturesObserver {
    fun onPropertyChange(p: PropertyChange, diff: Float)
}

internal class TouchGestures(private val observer: TouchGesturesObserver) {

    private enum class State {
        Up,
        Down,
        ControlSeek,
        ControlVolume,
        ControlBright,
        ControlSpeed,
    }

    private var state = State.Up
    // relevant movement direction for the current state (0=H, 1=V)
    private var stateDirection = 0

    // timestamp of the last tap (ACTION_UP)
    private var lastTapTime = 0L
    // when the current gesture began
    private var lastDownTime = 0L

    // where user initially placed their finger (ACTION_DOWN)
    private var initialPos = PointF()
    // last non-throttled processed position
    private var lastPos = PointF()

    private var width = 0f
    private var height = 0f
    // minimum movement which triggers a Control state
    private var trigger = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var holdSpeedEnabled = true
    private var holdSpeedIndex = SPEEDS.indexOf(2f)
    private var speedAnchor = 0f
    private var speedStepLeft = 0f
    private var speedStepRight = 0f
    private var speedVerticalAnchor = 0f
    private var keepSpeed = false
    private val startSpeedHold = Runnable {
        if (state == State.Down) {
            state = State.ControlSpeed
            lastTapTime = 0
            speedAnchor = lastPos.x
            speedVerticalAnchor = lastPos.y
            keepSpeed = false
            val step = min(width, height) / SPEED_STEP_RATE
            // Freeze directional thresholds for this hold; never shorten a step by more than 30%.
            speedStepLeft = ((speedAnchor - trigger) / holdSpeedIndex.coerceAtLeast(1))
                .coerceIn(step * 0.7f, step)
            speedStepRight = ((width - speedAnchor - trigger) /
                (SPEEDS.lastIndex - holdSpeedIndex).coerceAtLeast(1))
                .coerceIn(step * 0.7f, step)
            sendPropertyChange(PropertyChange.SpeedInit, SPEEDS[holdSpeedIndex])
        }
    }

    // which property change should be invoked where
    private var gestureHoriz = State.Down
    private var gestureVertLeft = State.Down
    private var gestureVertRight = State.Down
    private var tapGestureLeft : PropertyChange? = null
    private var tapGestureCenter : PropertyChange? = null
    private var tapGestureRight : PropertyChange? = null

    private inline fun checkFloat(vararg n: Float): Boolean {
        return !n.any { it.isInfinite() || it.isNaN() }
    }
    private inline fun assertFloat(vararg n: Float) {
        if (!checkFloat(*n))
            throw IllegalArgumentException()
    }

    fun setMetrics(width: Float, height: Float) {
        assertFloat(width, height)
        cancel()
        this.width = width
        this.height = height
        trigger = min(width, height) / TRIGGER_RATE
    }

    companion object {
        private const val TAG = "mpv"

        // ratio for trigger, 1/Xth of minimum dimension
        // for tap gestures this is the distance that must *not* be moved for it to trigger
        private const val TRIGGER_RATE = 30

        // maximum duration between taps (ms) for a double tap to count
        private const val TAP_DURATION = 300L
        val SPEEDS = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 4f)
        private const val SPEED_STEP_RATE = 8
        private const val KEEP_SPEED_DISTANCE_FRACTION = 0.1875f

        // full sweep from left side to right side is 2:30
        private const val CONTROL_SEEK_MAX = 150f

        // same as below, we rescale it inside MPVActivity
        private const val CONTROL_VOLUME_MAX = 1.5f

        // brightness is scaled 0..1; max's not 1f so that user does not have to start from the bottom
        // if they want to go from none to full brightness
        private const val CONTROL_BRIGHT_MAX = 1.5f

        // do not trigger on X% of screen top/bottom
        // this is so that user can open android status bar
        private const val DEADZONE = 5
    }

    private fun processTap(p: PointF): Boolean {
        if (state == State.Up) {
            lastDownTime = SystemClock.uptimeMillis()
            // 3 is another arbitrary value here that seems good enough
            if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() > trigger * 3)
                lastTapTime = 0 // last tap was too far away, invalidate
            return true
        }
        // discard if any movement gesture took place
        if (state != State.Down)
            return false

        val now = SystemClock.uptimeMillis()
        if (now - lastDownTime >= TAP_DURATION) {
            lastTapTime = 0 // finger was held too long, reset
            return false
        }
        if (now - lastTapTime < TAP_DURATION) {
            // [ Left 28% ] [    Center    ] [ Right 28% ]
            if (p.x <= width * 0.28f)
                tapGestureLeft?.let { sendPropertyChange(it, -1f); return true }
            else if (p.x >= width * 0.72f)
                tapGestureRight?.let { sendPropertyChange(it, 1f); return true }
            else
                tapGestureCenter?.let { sendPropertyChange(it, 0f); return true }
            lastTapTime = 0
        } else {
            lastTapTime = now
        }
        return false
    }

    private fun processMovement(p: PointF): Boolean {
        if (state == State.ControlSpeed) {
            val step = if (p.x >= speedAnchor) speedStepRight else speedStepLeft
            val steps = ((p.x - speedAnchor) / step).toInt()
            if (steps != 0) {
                val index = (holdSpeedIndex + steps).coerceIn(SPEEDS.indices)
                if (index != holdSpeedIndex) {
                    holdSpeedIndex = index
                    sendPropertyChange(PropertyChange.Speed, SPEEDS[index])
                }
                speedAnchor += steps * step
            }
            // Discard outward travel at the limits so reversing direction responds immediately.
            if ((holdSpeedIndex == SPEEDS.lastIndex && p.x > speedAnchor) ||
                (holdSpeedIndex == 0 && p.x < speedAnchor))
                speedAnchor = p.x
            if (keepSpeed) {
                speedVerticalAnchor = max(speedVerticalAnchor, p.y)
                if (speedVerticalAnchor - p.y >= trigger) {
                    keepSpeed = false
                    speedVerticalAnchor = p.y
                    sendPropertyChange(PropertyChange.SpeedKeep, 0f)
                }
            } else {
                speedVerticalAnchor = min(speedVerticalAnchor, p.y)
                if (p.y - speedVerticalAnchor >= min(width, height) * KEEP_SPEED_DISTANCE_FRACTION) {
                    keepSpeed = true
                    speedVerticalAnchor = p.y
                    sendPropertyChange(PropertyChange.SpeedKeep, 1f)
                }
            }
            return true
        }
        if (state == State.Down &&
            (abs(p.x - initialPos.x) > trigger || abs(p.y - initialPos.y) > trigger))
            handler.removeCallbacks(startSpeedHold)

        // throttle events: only send updates when there's some movement compared to last update
        // 3 here is arbitrary
        if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() < trigger / 3)
            return false
        lastPos.set(p)

        assertFloat(initialPos.x, initialPos.y)
        val dx = p.x - initialPos.x
        val dy = p.y - initialPos.y
        val dr = if (stateDirection == 0) (dx / width) else (-dy / height)

        when (state) {
            State.Up -> {}
            State.Down -> {
                // we might get into one of Control states if user moves enough
                if (abs(dx) > trigger) {
                    state = gestureHoriz
                    stateDirection = 0
                } else if (abs(dy) > trigger) {
                    state = if (initialPos.x > width / 2) gestureVertRight else gestureVertLeft
                    stateDirection = 1
                }
                // send Init so that it has a chance to cache values before we start modifying them
                if (state != State.Down)
                    sendPropertyChange(PropertyChange.Init, 0f)
            }
            State.ControlSeek ->
                sendPropertyChange(PropertyChange.Seek, CONTROL_SEEK_MAX * dr)
            State.ControlVolume ->
                sendPropertyChange(PropertyChange.Volume, CONTROL_VOLUME_MAX * dr)
            State.ControlBright ->
                sendPropertyChange(PropertyChange.Bright, CONTROL_BRIGHT_MAX * dr)
            State.ControlSpeed -> {}
        }
        return state != State.Up && state != State.Down
    }

    private fun sendPropertyChange(p: PropertyChange, diff: Float) {
        observer.onPropertyChange(p, diff)
    }

    fun syncSettings(prefs: SharedPreferences, resources: Resources) {
        cancel()
        holdSpeedEnabled = prefs.getBoolean("gesture_hold_speed", true)
        val savedSpeed = prefs.getFloat("gesture_hold_speed_value", 2f)
        holdSpeedIndex = SPEEDS.indexOf(savedSpeed)
        if (holdSpeedIndex == -1) {
            Log.w(TAG, "TouchGestures: invalid saved hold speed $savedSpeed")
            holdSpeedIndex = SPEEDS.indexOf(2f)
        }
        val get: (String, Int) -> String = { key, defaultRes ->
            val v = prefs.getString(key, "")
            if (v.isNullOrEmpty()) resources.getString(defaultRes) else v
        }
        val map = mapOf(
            "bright" to State.ControlBright,
            "seek" to State.ControlSeek,
            "volume" to State.ControlVolume
        )
        val map2 = mapOf(
            "seek" to PropertyChange.SeekFixed,
            "playpause" to PropertyChange.PlayPause,
            "custom" to PropertyChange.Custom
        )

        gestureHoriz = map[get("gesture_horiz", R.string.pref_gesture_horiz_default)] ?: State.Down
        gestureVertLeft = map[get("gesture_vert_left", R.string.pref_gesture_vert_left_default)] ?: State.Down
        gestureVertRight = map[get("gesture_vert_right", R.string.pref_gesture_vert_right_default)] ?: State.Down
        tapGestureLeft = map2[get("gesture_tap_left", R.string.pref_gesture_tap_left_default)]
        tapGestureCenter = map2[get("gesture_tap_center", R.string.pref_gesture_tap_center_default)]
        tapGestureRight = map2[get("gesture_tap_right", R.string.pref_gesture_tap_right_default)]
    }

    fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_CANCEL ||
            e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val handled = state != State.Up
            cancel()
            return handled
        }
        if (width < 1 || height < 1) {
            Log.w(TAG, "TouchGestures: width or height not set!")
            return false
        }
        if (!checkFloat(e.x, e.y)) {
            Log.w(TAG, "TouchGestures: ignoring invalid point ${e.x} ${e.y}")
            cancel()
            return false
        }
        var gestureHandled = false
        val point = PointF(e.x, e.y)
        when (e.actionMasked) {
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(startSpeedHold)
                if (state == State.Up)
                    return false
                gestureHandled = processMovement(point) or processTap(point)
                finishControl(commitSpeed = keepSpeed)
                state = State.Up
            }
            MotionEvent.ACTION_DOWN -> {
                if (state != State.Up)
                    cancel()
                // deadzone on top/bottom
                if (e.y < height * DEADZONE / 100 || e.y > height * (100 - DEADZONE) / 100)
                    return false
                initialPos.set(point)
                processTap(point)
                lastPos.set(point)
                state = State.Down
                if (holdSpeedEnabled)
                    handler.postDelayed(startSpeedHold, ViewConfiguration.getLongPressTimeout().toLong())
                // always return true on ACTION_DOWN to continue receiving events
                gestureHandled = true
            }
            MotionEvent.ACTION_MOVE -> {
                gestureHandled = processMovement(point)
            }
        }
        return gestureHandled
    }

    private fun finishControl(commitSpeed: Boolean = false) {
        when (state) {
            State.ControlSpeed -> sendPropertyChange(
                if (commitSpeed) PropertyChange.SpeedCommit else PropertyChange.SpeedFinalize,
                SPEEDS[holdSpeedIndex]
            )
            State.ControlSeek, State.ControlVolume, State.ControlBright ->
                sendPropertyChange(PropertyChange.Finalize, 0f)
            else -> {}
        }
    }

    fun cancel() {
        handler.removeCallbacks(startSpeedHold)
        finishControl()
        state = State.Up
        lastTapTime = 0
    }
}
