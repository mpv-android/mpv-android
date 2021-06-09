package `is`.xyz.mpv

import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StringRes

class SliderPickerDialog(
    private val rangeMin: Double, private val rangeMax: Double, private val intScale: Int,
    @StringRes private val formatTextRes: Int
) : PickerDialog {
    private lateinit var view: View

    private fun unscale(it: Int): Double = rangeMin + it.toDouble() / intScale

    private fun scale(it: Double): Int = ((it - rangeMin) * intScale).toInt()

    override fun buildView(layoutInflater: LayoutInflater): View {
        view = layoutInflater.inflate(R.layout.dialog_slider, null)
        val textView = view.findViewById<TextView>(R.id.textView)

        with (view.findViewById<SeekBar>(R.id.seekBar)) {
            max = ((rangeMax - rangeMin) * intScale).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    val progress = unscale(p1)
                    textView.text = view.context.getString(formatTextRes, progress)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }

        return view
    }

    override fun isInteger(): Boolean = intScale == 1

    override var number: Double?
        set(v) {
            view.findViewById<SeekBar>(R.id.seekBar).progress = scale(v!!)
            view.findViewById<TextView>(R.id.textView).text = view.context.getString(formatTextRes, v)
        }
        get() = unscale(view.findViewById<SeekBar>(R.id.seekBar).progress)
}