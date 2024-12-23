package `is`.xyz.mpv.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.ScalerPrefBinding

class ScalerDialogPreference(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    private var entries: Array<String>
    private lateinit var binding: ScalerPrefBinding

    private lateinit var s: Spinner
    private lateinit var e1: EditText
    private lateinit var e2: EditText

    init {
        isPersistent = false

        // read list of scalers from specified resource
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ScalerPreferenceDialog)
        val res = styledAttrs.getResourceId(R.styleable.ScalerPreferenceDialog_entries, -1)
        entries = context.resources.getStringArray(res)

        styledAttrs.recycle()
    }

    override fun onClick() {
        super.onClick()
        val dialog = AlertDialog.Builder(context)
        binding = ScalerPrefBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)
        setupViews()
        dialog.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        dialog.setPositiveButton(R.string.dialog_ok) { _, _ -> save() }
        dialog.create().show()
    }

    private fun setupViews() {
        s = binding.scaler
        e1 = binding.param1
        e2 = binding.param2

        // populate Spinner and set selected item
        s.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, entries).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val va = sharedPreferences?.getString(key, "") ?: ""
        val idx = entries.indexOf(va)
        if (idx != -1)
            s.setSelection(idx, false)

        // populate EditText's
        e1.setText(sharedPreferences?.getString("${key}_param1", "") ?: "")
        e2.setText(sharedPreferences?.getString("${key}_param2", "") ?: "")
    }

    private fun save() {
        val e = sharedPreferences?.edit()
        e?.putString(key, s.selectedItem as String)
        e?.putString("${key}_param1", e1.text.toString())
        e?.putString("${key}_param2", e2.text.toString())
        e?.apply()
    }
}
