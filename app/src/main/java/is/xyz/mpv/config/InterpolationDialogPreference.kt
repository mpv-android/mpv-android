package `is`.xyz.mpv.config

import `is`.xyz.mpv.R
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch

class InterpolationDialogPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
        defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var entries: Array<String>
    private var entryDefault: String

    init {
        isPersistent = false
        dialogLayoutResource = R.layout.interpolation_pref

        // read video sync modes
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.InterpolationPreferenceDialog)
        val res = styledAttrs.getResourceId(R.styleable.InterpolationPreferenceDialog_sync_entries, -1)
        entries = context.resources.getStringArray(res)
        entryDefault = styledAttrs.getString(R.styleable.InterpolationPreferenceDialog_sync_default) ?: ""

        styledAttrs.recycle()
    }

    private lateinit var myView: View

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        myView = view

        val sw = myView.findViewById<Switch>(R.id.switch1)
        val sp = myView.findViewById<Spinner>(R.id.video_sync)

        // populate switch
        sw.isChecked = sharedPreferences.getBoolean("${key}_interpolation", false)

        // populate spinner
        sp.adapter = ArrayAdapter(context, R.layout.scaler_pref_textview, entries)
        val idx = entries.indexOf(sharedPreferences.getString("${key}_sync", entryDefault))
        if (idx != -1)
            sp.setSelection(idx, false)

        // set listeners
        sw.setOnCheckedChangeListener { _, state -> ensureSyncMode(state) }
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent!!.adapter.getItem(position) as String
                ensureInterpolationToggled(item)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (!positiveResult)
            return

        val sw = myView.findViewById<Switch>(R.id.switch1)
        val sp = myView.findViewById<Spinner>(R.id.video_sync)

        val e = editor // new SharedPreferences.Editor instance
        e.putBoolean("${key}_interpolation", sw.isChecked)
        e.putString("${key}_sync", sp.selectedItem as String)
        e.commit()
    }

    // ensure setting consistency when either of switch/spinner changes

    private fun ensureSyncMode(interpolationState: Boolean) {
        if (!interpolationState)
            return
        val sp = myView.findViewById<Spinner>(R.id.video_sync)

        if (!(sp.selectedItem as String).startsWith("display-")) {
            val idx = entries.indexOfFirst { s -> s.startsWith("display-") }
            sp.setSelection(idx, true)
        }
    }

    private fun ensureInterpolationToggled(syncMode: String) {
        if (syncMode.startsWith("display-"))
            return
        val sw = myView.findViewById<Switch>(R.id.switch1)
        sw.isChecked = false
    }
}
