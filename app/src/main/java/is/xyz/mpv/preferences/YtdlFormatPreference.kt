package `is`.xyz.mpv.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.YtdlFormatPrefBinding

class YtdlFormatPreference(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private lateinit var binding: YtdlFormatPrefBinding

    init {
        isPersistent = true
    }

    override fun onClick() {
        super.onClick()
        val sp = sharedPreferences!!

        val dialog = AlertDialog.Builder(context)
        binding = YtdlFormatPrefBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)
        dialog.setTitle(title)
        setupViews(sp)
        dialog.setNegativeButton(R.string.dialog_cancel, null)
        dialog.setPositiveButton(R.string.dialog_save) { _, _ -> save(sp) }
        dialog.create().show()
    }

    private fun setupViews(sp: android.content.SharedPreferences) {
        val qstr = sp.getString(key, "") ?: ""
        val selectedQuality = qstr.substringAfter("[height<=?").substringBefore("]").toIntOrNull() ?: -1
        val preferH264 = qstr.contains("[vcodec^=?avc]")

        binding.switch1.isChecked = preferH264

        val qualityStrings = qualityLevels.map {
            if (it == -1) context.getString(R.string.quality_any) else "${it}p"
        }
        binding.spinner1.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, qualityStrings).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val idx = qualityLevels.indexOf(selectedQuality)
        if (idx != -1)
            binding.spinner1.setSelection(idx, false)
    }

    private fun save(sp: android.content.SharedPreferences) {
        val selectedQuality = qualityLevels[binding.spinner1.selectedItemPosition]
        val preferH264 = binding.switch1.isChecked

        var qstr = ""
        /* bv = bestvideo, ba = bestaudio, b = best */
        if (selectedQuality != -1 && preferH264) {
            qstr = "(bv*[vcodec^=?avc]/bv*[vcodec^=?mp4])[height<=?${selectedQuality}]+ba/" +
                    "(b[vcodec^=?avc]/b[vcodec^=?mp4])[height<=?${selectedQuality}]"
        } else if (selectedQuality != -1) {
            qstr = "bv[height<=?${selectedQuality}]+ba/b[height<=?${selectedQuality}]"
        } else if (preferH264) {
            qstr = "(bv*[vcodec^=?avc]/bv*[vcodec^=?mp4])+ba/(b[vcodec^=?avc]/b[vcodec^=?mp4])"
        }
        if (qstr.isNotEmpty())
            qstr += "/bv*+ba/b"

        with(sp.edit()) {
            putString(key, qstr)
            apply()
        }
    }

    companion object {
        private val qualityLevels = arrayOf(-1, 1440, 1080, 720, 480, 360, 240, 144)
    }
}
