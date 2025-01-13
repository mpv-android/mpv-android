package `is`.xyz.mpv.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
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
    private lateinit var sp: MaterialAutoCompleteTextView

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
        dialog.setTitle(title)
        setupViews()
        dialog.setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        dialog.setPositiveButton(R.string.dialog_ok) { _, _ -> save() }
        dialog.create().show()
    }

    private fun setupViews() {
        sw = binding.switch1
        sp = binding.videoSync as MaterialAutoCompleteTextView

        // populate switch
        sw.isChecked = sharedPreferences?.getBoolean("${key}_interpolation", false) ?: false

        // populate spinner
        val s = sharedPreferences?.getString("${key}_sync", entryDefault) ?: entryDefault
        sp.setText(s, false)
        sp.setSimpleItems(entries)

        // set listeners
        sw.setOnCheckedChangeListener { _, state -> ensureSyncMode(state) }
        sp.addTextChangedListener { ensureInterpolationToggled() }
    }

    private fun save() {
        val e = sharedPreferences?.edit()
        e?.putBoolean("${key}_interpolation", sw.isChecked)
        e?.putString("${key}_sync", sp.text.toString())
        e?.apply()
    }

    // ensure setting consistency when either of switch/spinner changes
    private fun ensureSyncMode(interpolationState: Boolean) {
        if (!interpolationState) return

        if (!sp.text.startsWith("display-")) {
            val s = entries.first { s -> s.startsWith("display-") }
            sp.setText(s, false)
        }
    }

    private fun ensureInterpolationToggled() {
        if (sp.text.startsWith("display-"))
            return
        sw.isChecked = false
    }
}
