package `is`.xyz.mpv

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText

class DecimalPickerDialog(
    private val rangeMin: Double, private val rangeMax: Double
) : PickerDialog {
    private lateinit var view: View

    override fun buildView(layoutInflater: LayoutInflater): View {
        view = layoutInflater.inflate(R.layout.dialog_decimal, null)
        val editText = view.findViewById<EditText>(R.id.editText)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val value = s!!.toString().toDoubleOrNull() ?: return
                val valueBounded = value.coerceIn(rangeMin, rangeMax)
                if (valueBounded != value)
                    editText.setText(valueBounded.toString())
            }
        })
        view.findViewById<Button>(R.id.btnMinus).setOnClickListener {
            val value = this.number ?: 0.0
            this.number = (value - STEP).coerceIn(rangeMin, rangeMax)
        }
        view.findViewById<Button>(R.id.btnPlus).setOnClickListener {
            val value = this.number ?: 0.0
            this.number = (value + STEP).coerceIn(rangeMin, rangeMax)
        }

        return view
    }

    override fun isInteger(): Boolean = false

    override var number: Double?
        set(v) = view.findViewById<EditText>(R.id.editText).setText(v!!.toString())
        get() = view.findViewById<EditText>(R.id.editText).text.toString().toDoubleOrNull()


    companion object {
        private const val STEP = 1.0
    }
}