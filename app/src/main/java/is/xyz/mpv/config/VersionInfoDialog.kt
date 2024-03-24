package `is`.xyz.mpv.config

import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.R
import android.content.Context
import android.util.AttributeSet
import android.preference.DialogPreference
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView

class VersionInfoDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes), MPVLib.LogObserver {
    init {
        isPersistent = false
        dialogLayoutResource = R.layout.version_dialog
    }

    private lateinit var myView: View
    private lateinit var versionText: String

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        myView = view

        versionText = "mpv-android ${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n"
        /* create mpv context to capture version info from log */
        MPVLib.create(context)
        MPVLib.addLogObserver(this)
        MPVLib.init()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        MPVLib.destroy()
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer")
            return
        if (level == MPVLib.mpvLogLevel.MPV_LOG_LEVEL_V)
            versionText += text
        if (text.startsWith("List of enabled features:")) {
            /* stop receiving log messages and populate text field */
            MPVLib.removeLogObserver(this)
            val field = myView.findViewById<TextView>(R.id.info)
            (context as SettingsActivity).runOnUiThread {
                field.text = versionText
            }
        }
    }
}
