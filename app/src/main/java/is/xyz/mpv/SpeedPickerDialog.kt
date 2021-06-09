package `is`.xyz.mpv

import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.max

class SpeedPickerDialog : PickerDialog {
    companion object {
        // Middle point of bar (in progress units)
        private const val HALF = 100.0
        // Minimum for <1.0 range (absolute)
        private const val MINIMUM = 0.2
        // Scale factor for >=1.0 range (in progress units)
        private const val SCALE_FACTOR = 20.0
    }

    private lateinit var view: View

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

    override fun buildView(layoutInflater: LayoutInflater): View {
        view = layoutInflater.inflate(R.layout.dialog_slider, null)
        val textView = view.findViewById<TextView>(R.id.textView)

        with (view.findViewById<SeekBar>(R.id.seekBar)) {
            max = 200
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val progress = toSpeed(p1)
                    textView.text = view.context.getString(R.string.ui_speed, progress)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        textView.isAllCaps = true // match appearance in controls

        return view
    }

    override fun isInteger(): Boolean = false

    override var number: Double?
        set(v) {
            view.findViewById<SeekBar>(R.id.seekBar).progress = fromSpeed(v!!)
            view.findViewById<TextView>(R.id.textView).text = view.context.getString(R.string.ui_speed, v)
        }
        get() = toSpeed(view.findViewById<SeekBar>(R.id.seekBar).progress)
}