package `is`.xyz.mpv

import android.graphics.PointF
import android.view.MotionEvent
import java.lang.Math

enum class PropertyChange {
    Init,
    Seek,
    Volume,
    Bright,
    Finalize
}

interface TouchGesturesObserver {
    fun onPropertyChange(p: PropertyChange, diff: Float);
}

class TouchGestures(val width: Float, val height: Float, val observer: TouchGesturesObserver) {

    enum class State {
        Up,
        Down,
        ControlSeek,
        ControlVolume,
        ControlBright,
    }

    private var state = State.Up
    private var initialPos = PointF()

    // minimum movement which triggers a Control state
    private var trigger: Float
    // ratio for trigger, 1/10 of minimum dimension
    private val TRIGGER_RATE = 10

    // full sweep from left side to right side is 2:30
    private val CONTROL_SEEK_MAX = 150
    private val CONTROL_VOLUME_MAX = 200
    private val CONTROL_BRIGHT_MAX = 200

    init {
        trigger = Math.min(width, height) / TRIGGER_RATE
    }

    private fun processMovement(p: PointF) {
        val dx = p.x - initialPos.x
        val dy = p.y - initialPos.y

        // TODO: throttle events, i.e. only send updates when there's some movement compared to last update

        when (state) {
            State.Up -> {}
            State.Down -> {
                // we might get into one of Control states if user moves enough
                if (Math.abs(dx) > trigger) {
                    state = State.ControlSeek
                } else if (Math.abs(dy) > trigger) {
                    // depending on left/right side we might want volume or brightness control
                    if (initialPos.x > width / 2)
                        state = State.ControlVolume
                    else
                        state = State.ControlBright
                }
                // send Init so that it has a chance to cache values before we start modifying them
                if (state != State.Down)
                    sendPropertyChange(PropertyChange.Init, 0f)
            }
            State.ControlSeek ->
                sendPropertyChange(PropertyChange.Seek, CONTROL_SEEK_MAX * dx / width)
            State.ControlVolume ->
                sendPropertyChange(PropertyChange.Volume, -CONTROL_VOLUME_MAX * dy / height)
            State.ControlBright ->
                sendPropertyChange(PropertyChange.Bright, -CONTROL_BRIGHT_MAX * dy / height)
        }
    }

    private fun sendPropertyChange(p: PropertyChange, diff: Float) {
        observer.onPropertyChange(p, diff)
    }

    fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_UP -> {
                processMovement(PointF(e.x, e.y))
                sendPropertyChange(PropertyChange.Finalize, 0f)
                state = State.Up
            }
            MotionEvent.ACTION_DOWN -> {
                initialPos = PointF(e.x, e.y)
                state = State.Down
            }
            MotionEvent.ACTION_MOVE -> {
                processMovement(PointF(e.x, e.y))
            }
        }

        return true
    }
}