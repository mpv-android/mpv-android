package `is`.xyz.mpv.config

import `is`.xyz.mpv.R
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

class ScalerDialogPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
        defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var entries: Array<String>

    init {
        isPersistent = false
        dialogLayoutResource = R.layout.scaler_pref

        // read list of scalers from specified resource
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ScalerPreferenceDialog)
        val res = styledAttrs.getResourceId(R.styleable.ScalerPreferenceDialog_entries, -1)
        entries = context.resources.getStringArray(res)

        styledAttrs.recycle()
    }

    private lateinit var myView: View

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        myView = view

        val s = myView.findViewById<Spinner>(R.id.scaler)
        val e1 = myView.findViewById<EditText>(R.id.param1)
        val e2 = myView.findViewById<EditText>(R.id.param2)

        // populate Spinner and set selected item
        s.adapter = ArrayAdapter<String>(context, R.layout.scaler_pref_textview, entries)
        val va = sharedPreferences.getString(key, "")
        val idx = entries.indexOf(va)
        if (idx != -1)
            s.setSelection(idx, false)

        // populate EditText's
        e1.setText(sharedPreferences.getString("${key}_param1", ""))
        e2.setText(sharedPreferences.getString("${key}_param2", ""))
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        // save values only if user presses OK
        if (!positiveResult)
            return

        val s = myView.findViewById<Spinner>(R.id.scaler)
        val e1 = myView.findViewById<EditText>(R.id.param1)
        val e2 = myView.findViewById<EditText>(R.id.param2)

        val e = editor // Will create(!) a new SharedPreferences.Editor instance
        e.putString(key, s.selectedItem as String)
        e.putString("${key}_param1", e1.text.toString())
        e.putString("${key}_param2", e2.text.toString())
        e.commit()
    }
}