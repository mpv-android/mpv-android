package `is`.xyz.mpv

import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.max

object SpeedPickerDialog {
    // Middle point of bar (in progress units)
    private const val HALF = 100.0
    // Minimum for <1.0 range (absolute)
    private const val MINIMUM = 0.2
    // Scale factor for >=1.0 range (in progress units)
    private const val SCALE_FACTOR = 20.0

    private fun toSpeed(it: Int): Double {
        return if (it >= HALF)
            (it - HALF) / SCALE_FACTOR + 1.0
        else
            max(MINIMUM, it / HALF)
    }

    private fun fromSpeed(it: Double): Int {
        return if (it >= 1.0)
            (HALF + (it - 1.0) * SCALE_FACTOR).toInt()
        else
            (HALF * max(MINIMUM, it)).toInt()
    }

    fun buildView(layoutInflater: LayoutInflater, currentSpeed: Double): View {
        val view = layoutInflater.inflate(R.layout.dialog_speed, null)

        with (view.findViewById<SeekBar>(R.id.seekBar)) {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val progress = toSpeed(p1)
                    view.findViewById<TextView>(R.id.textView).text = "${progress}x"
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
            progress = fromSpeed(currentSpeed)
        }
        view.findViewById<TextView>(R.id.textView).text = "${currentSpeed}x"

        return view
    }

    fun readResult(view: View): Double {
        return toSpeed(view.findViewById<SeekBar>(R.id.seekBar).progress)
    }
}