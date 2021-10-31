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
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.Enumeration
import java.util.Base64
import java.io.StringWriter
import java.io.FileWriter
import java.io.File

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
        infoText.setText("Generating database...")
    
        val fw: FileWriter = FileWriter(configFile.path)

        val ks: KeyStore = KeyStore.getInstance("AndroidCAStore").apply {
            load(null)
        }

        val aliases: Enumeration<String> = ks.aliases()

        while (aliases.hasMoreElements()) {
            val alias: String = aliases.nextElement()
            val cert: Certificate = ks.getCertificate(alias)

            val encoder: Base64.Encoder = Base64.getMimeEncoder(64, "\n".getBytes())
            var rawCert: byte[] = cert.getEncoded()
            val certText: String(encoder.encode(rawCert))

            val sw: StringWriter = StringWriter()
            sw.write("-----BEGIN CERTIFICATE-----\n")
            sw.write(certText)
            sw.write("\n-----END CERTIFICATE-----\n")

            fw.write(sw.toString())
        }

        fw.close()

        infoText.setText(caFile.readText())
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
    }
}