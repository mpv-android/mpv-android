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

class GenerateCertificateDatabase @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var caFile: File

    init {
        isPersistent = false
        dialogLayoutResource = R.layout.dialog_generateca

        configFile = File("${context.filesDir.path}/cacert.pem")
    }

    private lateinit var myView: View

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        myView = view

        val infoText = view.findViewById<TextView>(R.id.info)
        if (configFile.exists())
            infoText.setText(caFile.readText())
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
    }
}