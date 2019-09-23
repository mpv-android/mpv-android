package `is`.xyz.mpv

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.DialogPreference
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.IllegalFormatException

class CustomizableButtonDialogPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
        defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var entries: Array<String>
    private var values: Array<String>

    init {
        isPersistent = false
        dialogLayoutResource = R.layout.custombtn_pref

        entries = context.resources.getStringArray(R.array.custombtn_appearance_entries)
        values = context.resources.getStringArray(R.array.custombtn_appearance_values)
    }

    private lateinit var myView: View

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        myView = view
        val prefs = sharedPreferences

        val v_cmd = myView.findViewById<EditText>(R.id.command)
        val v_app = myView.findViewById<Spinner>(R.id.appearance)
        val v_static = myView.findViewById<EditText>(R.id.static_text)
        val v_propname = myView.findViewById<EditText>(R.id.property_name)
        val v_propnum = myView.findViewById<CheckBox>(R.id.property_numeric)
        val v_propfmt = myView.findViewById<EditText>(R.id.property_format)

        // spinner
        v_app.adapter = ArrayAdapter<String>(context, R.layout.scaler_pref_textview, entries)
        val va = prefs.getString("${key}_appearance", "")
        var idx = values.indexOf(va)
        if (idx == -1)
            idx = 0 // default to first
        v_app.setSelection(idx, false)
        appearanceSelected(values[idx])

        // text fields
        v_cmd.setText(prefs.getString("${key}_command", ""))
        v_static.setText(prefs.getString("${key}_static", ""))
        v_propname.setText(prefs.getString("${key}_property", ""))
        v_propfmt.setText(prefs.getString("${key}_format", ""))

        // checkbox
        v_propnum.isChecked = prefs.getBoolean("${key}_property_numeric", false)

        // set listeners
        v_app.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appearanceSelected(values[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        myView.findViewById<TextView>(R.id.helpBtn).setOnClickListener {
            val pad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f,
                    context.resources.displayMetrics).toInt() // 16dp

            val msg = TextView(context)
            msg.autoLinkMask = Linkify.WEB_URLS
            msg.linksClickable = true
            msg.setPadding(pad, 0, pad, 0)
            msg.setText(R.string.custombtn_help_text)

            val b = AlertDialog.Builder(context)
                    .setTitle(R.string.custombtn_help)
                    .setView(msg)
            b.setNeutralButton(R.string.dialog_ok) { _, _ -> }
            b.show()
        }
    }

    override fun showDialog(state: Bundle?) {
        super.showDialog(state)

        // custom OnClick listener so we can do data validation (thanks a lot google)
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                dialog.dismiss()
                onDialogClosed(true)
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (!positiveResult)
            return

        val v_cmd = myView.findViewById<EditText>(R.id.command)
        val v_app = myView.findViewById<Spinner>(R.id.appearance)
        val v_static = myView.findViewById<EditText>(R.id.static_text)
        val v_propname = myView.findViewById<EditText>(R.id.property_name)
        val v_propnum = myView.findViewById<CheckBox>(R.id.property_numeric)
        val v_propfmt = myView.findViewById<EditText>(R.id.property_format)

        val e = editor // new Editor instance
        e.putString("${key}_appearance", values[v_app.selectedItemPosition])
        e.putString("${key}_command", v_cmd.text.toString())
        e.putString("${key}_static", v_static.text.toString())
        e.putString("${key}_property", v_propname.text.toString())
        e.putBoolean("${key}_property_numeric", v_propnum.isChecked)
        e.putString("${key}_format", v_propfmt.text.toString())
        e.commit()
    }

    private fun validate(): Boolean {
        val v_app = myView.findViewById<Spinner>(R.id.appearance)
        val v_propnum = myView.findViewById<CheckBox>(R.id.property_numeric)
        val v_propfmt = myView.findViewById<EditText>(R.id.property_format)

        if (values[v_app.selectedItemPosition] == "property") {
            // test the format string
            val format = v_propfmt.text.toString()
            var res = ""
            try {
                res = if (v_propnum.isChecked)
                    String.format(format, 1.234)
                else
                    String.format(format, "abcd")
            } catch (e: IllegalFormatException) {
            }
            if (res == "") {
                v_propfmt.error = context.getString(R.string.error_format_str)
                return false
            }
        }

        return true
    }

    private fun setViewEnabled(view: View, enabled: Boolean) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount)
                setViewEnabled(view.getChildAt(i), enabled)
        } else {
            view.isEnabled = enabled
        }
    }
    private fun setRowEnabled(id: Int, enabled: Boolean) {
        setViewEnabled(myView.findViewById(id), enabled)
    }

    private fun appearanceSelected(appearance: String) {
        setRowEnabled(R.id.tableRow3, appearance == "static")
        setRowEnabled(R.id.tableRow4, appearance == "property")
        setRowEnabled(R.id.tableRow5, appearance == "property")
    }
}
