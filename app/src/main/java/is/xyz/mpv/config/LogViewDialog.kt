package `is`.xyz.mpv.config

import android.annotation.SuppressLint
import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.R
import android.content.Context
import android.util.AttributeSet
import android.preference.DialogPreference
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import `is`.xyz.mpv.MPVView
import java.util.Date

class LogViewDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        isPersistent = false
        dialogLayoutResource = R.layout.version_dialog
    }

    private lateinit var textView: TextView

    @SuppressLint("SetTextI18n")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        textView = view.findViewById(R.id.info)

        val path = MPVView.getLogPath(context)
        val mtime = path.lastModified()
        if (mtime == 0L) {
            textView.text = context.getString(R.string.log_file_unavailable) + "\n"
            return
        }
        val dateStr = DateFormat.getDateFormat(context).format(Date(mtime))
        val timeStr = DateFormat.getTimeFormat(context).format(Date(mtime))
        textView.text = context.getString(R.string.log_file_date, dateStr, timeStr) + "\n" + path.readText()
    }
}
