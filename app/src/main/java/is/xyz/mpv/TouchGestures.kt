package `is`.xyz.mpv

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import kotlin.math.*

enum class PropertyChange {
    Init,
    Seek,
    Volume,
    Bright,
    Finalize,

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

    private var width: Float = 0f
    private var height: Float = 0f
    // minimum movement which triggers a Control state
    private var trigger: Float = 0f

    // which property change should be invoked where
    private var gestureHoriz = State.Down
    private var gestureVertLeft = State.Down
    private var gestureVertRight = State.Down
    private var tapGestureLeft : PropertyChange? = null
    private var tapGestureCenter : PropertyChange? = null
    private var tapGestureRight : PropertyChange? = null

    fun setMetrics(width: Float, height: Float) {
        this.width = width
        this.height = height
        trigger = min(width, height) / TRIGGER_RATE
    }

    companion object {
        // ratio for trigger, 1/Xth of minimum dimension
        // for tap gestures this is the distance that must *not* be moved for it to trigger
        private const val TRIGGER_RATE = 30

        // maximum duration between taps (ms) for a double tap to count
        private const val TAP_DURATION = 300L

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
        // throttle events: only send updates when there's some movement compared to last update
        // 3 here is arbitrary
        if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() < trigger / 3)
            return false
        lastPos.set(p)

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
        }
        return state != State.Up && state != State.Down
    }

    private fun sendPropertyChange(p: PropertyChange, diff: Float) {
        observer.onPropertyChange(p, diff)
    }

    fun syncSettings(prefs: SharedPreferences, resources: Resources) {
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
        if (width == 0f || height == 0f)
            return false
        var gestureHandled = false
        val point = PointF(e.x, e.y)
        when (e.action) {
            MotionEvent.ACTION_UP -> {
                gestureHandled = processMovement(point) or processTap(point)
                if (state != State.Down)
                    sendPropertyChange(PropertyChange.Finalize, 0f)
                state = State.Up
                return gestureHandled
            }
            MotionEvent.ACTION_DOWN -> {
                // deadzone on top/bottom
                if (e.y < height * DEADZONE / 100 || e.y > height * (100 - DEADZONE) / 100)
                    return false
                processTap(point)
                initialPos = point
                lastPos.set(initialPos)
                state = State.Down
                // always return true on ACTION_DOWN to continue receiving events
                gestureHandled = true
            }
            MotionEvent.ACTION_MOVE -> {
                gestureHandled = processMovement(point)
            }
        }
        return gestureHandled
    }
}
