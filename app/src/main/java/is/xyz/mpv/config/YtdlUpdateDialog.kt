package `is`.xyz.mpv.config

import `is`.xyz.mpv.R
import android.content.Context
import android.preference.DialogPreference
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import kotlin.concurrent.thread

class YtdlUpdateDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        isPersistent = false
        dialogLayoutResource = R.layout.version_dialog
    }

    private var proc: Process? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        var text = "Running youtube-dl --update:\n"
        val field = view.findViewById<TextView>(R.id.info)
        field.text = text
        field.movementMethod = ScrollingMovementMethod()

        try {
            with(ProcessBuilder("${this.context.filesDir.path}/youtube-dl", "--update")) {
                redirectErrorStream(true)
                proc = start()
            }
        } catch (e: Exception) {
            text += e.message
            field.text = text
            return
        }

        thread {
            val buf = ByteArray(1024)
            do {
                val n = proc!!.inputStream.read(buf)
                if (n > 0) {
                    text += String(buf.copyOfRange(0, n))
                    view.postOnAnimation { field.text = text }
                }
            } while (n != -1)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        proc?.destroy()
    }
}
