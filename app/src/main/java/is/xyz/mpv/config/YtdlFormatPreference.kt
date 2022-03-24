package `is`.xyz.mpv.config

import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.YtdlFormatPrefBinding
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter

class YtdlFormatPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        isPersistent = false
        dialogLayoutResource = R.layout.ytdl_format_pref
    }

    private lateinit var binding: YtdlFormatPrefBinding

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        binding = YtdlFormatPrefBinding.bind(view)

        // parse current states from the string, a bit ugly but whatever
        val qstr = sharedPreferences.getString(key, "")!!
        val selectedQuality = qstr.substringAfter("[height<=?").substringBefore("]").toIntOrNull() ?: -1
        val preferH264 = qstr.contains("[vcodec^=?avc]")

        // populate elements
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

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (!positiveResult)
            return

        val selectedQuality = qualityLevels[binding.spinner1.selectedItemPosition]
        val preferH264 = binding.switch1.isChecked

        var qstr = ""
        if (selectedQuality != -1 && preferH264) {
            qstr = "(bestvideo[vcodec^=?avc]/bestvideo[vcodec^=?mp4])[height<=?${selectedQuality}]+bestaudio/" +
                    "([vcodec^=?avc]/[vcodec^=?mp4])[height<=?${selectedQuality}]"
        } else if (selectedQuality != -1) {
            qstr = "bestvideo[height<=?${selectedQuality}]+bestaudio/[height<=?${selectedQuality}]"
        } else if (preferH264) {
            qstr = "(bestvideo[vcodec^=?avc]/bestvideo[vcodec^=?mp4])+bestaudio/([vcodec^=?avc]/[vcodec^=?mp4])"
        }
        if (qstr.isNotEmpty())
            qstr += "/bestvideo+bestaudio/best"

        with (editor) {
            putString(key, qstr)
            commit()
        }
    }

    companion object {
        private val qualityLevels = arrayOf(-1, 1440, 1080, 720, 480)
    }
}
