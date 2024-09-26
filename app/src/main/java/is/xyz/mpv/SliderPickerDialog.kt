package `is`.xyz.mpv

import android.view.LayoutInflater
import android.view.View
import `is`.xyz.mpv.databinding.DialogSliderBinding

internal class SliderPickerDialog(
    private val rangeMin: Float,
    private val rangeMax: Float,
    private val stepSize: Float = 0.05f,
    private val default: Float = rangeMin + (rangeMax - rangeMin) / 2
) : PickerDialog {
    private lateinit var binding: DialogSliderBinding

    override fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogSliderBinding.inflate(layoutInflater)
        binding.slider.valueFrom = rangeMin
        binding.slider.valueTo = rangeMax
        binding.slider.stepSize = stepSize
        binding.slider.addOnChangeListener { _, value, _ ->
            binding.sliderValue.text = layoutInflater.context.getString(R.string.ui_speed, value)
        }
        return binding.root
    }

    override fun isInteger(): Boolean = stepSize % 1 == 0f
    override fun reset() {
        number = default.toDouble()
    }

    override var number: Double?
        get() = binding.slider.value.toDouble()
        set(v) {
            binding.sliderValue.text = binding.slider.context.getString(R.string.ui_speed, v)
            binding.slider.value = v!!.toFloat()
        }
}
