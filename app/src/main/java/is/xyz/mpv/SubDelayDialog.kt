package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.DialogDecimalBinding
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible

internal class SubDelayDialog(
    private val rangeMin: Double, private val rangeMax: Double
) {
    private lateinit var binding: DialogDecimalBinding

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogDecimalBinding.inflate(layoutInflater)

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) = handleTextChange(s?.toString(), binding.editText)
        })
        binding.editText2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) = handleTextChange(s?.toString(), binding.editText2)
        })
        val onClick1 = { delta: Double ->
            val value = this.delay1 ?: 0.0
            this.delay1 = (value + delta).coerceIn(rangeMin, rangeMax)
        }
        val onClick2 = { delta: Double ->
            val value = this.delay2 ?: 0.0
            this.delay2 = (value + delta).coerceIn(rangeMin, rangeMax)
        }
        binding.btnMinus.setOnClickListener { onClick1(-STEP) }
        binding.btnPlus.setOnClickListener { onClick1(STEP) }
        binding.btnMinus2.setOnClickListener { onClick2(-STEP) }
        binding.btnPlus2.setOnClickListener { onClick2(STEP) }

        return binding.root
    }

    private fun handleTextChange(s: String?, editText: EditText) {
        val value = s?.toDoubleOrNull() ?: return
        val valueBounded = value.coerceIn(rangeMin, rangeMax)
        if (valueBounded != value)
            editText.setText(valueBounded.toString())
    }

    /** Primary sub delay */
    var delay1: Double?
        set(v) = binding.editText.setText(v!!.toString())
        get() = binding.editText.text.toString().toDoubleOrNull()

    /**
     * Secondary sub delay. Set to null to hide related UI parts.
     */
    var delay2: Double?
        set(v) {
            arrayOf(binding.label1, binding.label2, binding.rowSecondary).forEach {
                it.isVisible = v != null
            }
            if (v != null)
                binding.editText2.setText(v.toString())
        }
        get() = binding.editText2.text.toString().toDoubleOrNull()

    companion object {
        private const val STEP = 1.0
    }
}
