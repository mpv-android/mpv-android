package `is`.xyz.mpv.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.material.materialswitch.MaterialSwitch
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.InterpolationPrefBinding


class InterpolationDialogPreference(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {
    private var entries: Array<String>
    private var entryDefault: String
    private lateinit var binding: InterpolationPrefBinding

    private lateinit var sw: MaterialSwitch
    private lateinit var sp: Spinner

    init {
        isPersistent = false

        // read video sync modes
        val styledAttrs = context.obtainStyledAttributes(
            attrs, R.styleable.InterpolationPreferenceDialog
        )
        val res = styledAttrs.getResourceId(
            R.styleable.InterpolationPreferenceDialog_sync_entries, -1
        )
        entries = context.resources.getStringArray(res)
        entryDefault =
            styledAttrs.getString(R.styleable.InterpolationPreferenceDialog_sync_default)
                ?: ""

        styledAttrs.recycle()
    }

    override fun onClick() {
        super.onClick()
        val dialog = AlertDialog.Builder(context)
        binding = InterpolationPrefBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)
        setupViews()
        dialog.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        dialog.setPositiveButton(R.string.dialog_ok) { _, _ -> save() }
        dialog.create().show()
    }


    private fun setupViews() {

        sw = binding.switch1
        sp = binding.videoSync

        // populate switch
        sw.isChecked = sharedPreferences?.getBoolean("${key}_interpolation", false) ?: false

        // populate spinner
        sp.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, entries).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val idx = entries.indexOf(
            sharedPreferences?.getString("${key}_sync", entryDefault) ?: entryDefault
        )
        if (idx != -1) sp.setSelection(idx, false)

        // set listeners
        sw.setOnCheckedChangeListener { _, state -> ensureSyncMode(state) }
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val item = parent!!.adapter.getItem(position) as String
                ensureInterpolationToggled(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun save() {
        val e = sharedPreferences?.edit()
        e?.putBoolean("${key}_interpolation", sw.isChecked)
        e?.putString("${key}_sync", sp.selectedItem as String)
        e?.apply()
    }

    // ensure setting consistency when either of switch/spinner changes
    private fun ensureSyncMode(interpolationState: Boolean) {
        if (!interpolationState) return

        if (!(sp.selectedItem as String).startsWith("display-")) {
            val idx = entries.indexOfFirst { s -> s.startsWith("display-") }
            sp.setSelection(idx, true)
        }
    }

    private fun ensureInterpolationToggled(syncMode: String) {
        if (syncMode.startsWith("display-")) return
        sw.isChecked = false
    }
}