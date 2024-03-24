package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.DialogSliderBinding
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.annotation.StringRes

internal class SliderPickerDialog(
    private val rangeMin: Double, private val rangeMax: Double, private val intScale: Int,
    @StringRes private val formatTextRes: Int
) : PickerDialog {
    private lateinit var binding: DialogSliderBinding

    private fun unscale(it: Int): Double = rangeMin + it.toDouble() / intScale

    private fun scale(it: Double): Int = ((it - rangeMin) * intScale).toInt()

    override fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogSliderBinding.inflate(layoutInflater)
        val context = layoutInflater.context

        binding.seekBar.max = ((rangeMax - rangeMin) * intScale).toInt()
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val progress = unscale(p1)
                binding.textView.text = context.getString(formatTextRes, progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        binding.resetBtn.setOnClickListener {
            number = rangeMin + (rangeMax - rangeMin) / 2 // works for us
        }

        return binding.root
    }

    override fun isInteger(): Boolean = intScale == 1

    override var number: Double?
        set(v) { binding.seekBar.progress = scale(v!!) }
        get() = unscale(binding.seekBar.progress)
}
