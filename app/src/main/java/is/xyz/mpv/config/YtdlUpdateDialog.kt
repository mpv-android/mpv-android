package `is`.xyz.mpv.config

import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.databinding.YtdlUpdatePrefBinding
import android.content.Context
import android.preference.DialogPreference
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.View
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class YtdlUpdateDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
): DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        isPersistent = false
        dialogLayoutResource = R.layout.ytdl_update_pref
    }

    private lateinit var binding: YtdlUpdatePrefBinding
    private var proc: Process? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        binding = YtdlUpdatePrefBinding.bind(view)

        Utils.copyAssets(context)

        binding.info.movementMethod = ScrollingMovementMethod()
        binding.installBtn.setOnClickListener {
            runProcess("Installing yt-dlp:", mutableListOf("ytdl/wrapper", "setup.py"))
        }
        binding.updateBtn.setOnClickListener {
            runProcess("Running youtube-dl --update:", mutableListOf("youtube-dl", "--update"))
        }
        binding.updateBtn.isEnabled = File("${context.filesDir.path}/youtube-dl").exists()
    }

    private fun runProcess(initText: String, command: MutableList<String>)
    {
        if (proc != null)
            return

        var text = initText + "\n"
        binding.info.text = text

        command[0] = "${context.filesDir.path}/${command[0]}"
        try {
            with (ProcessBuilder(command)) {
                redirectErrorStream(true)
                proc = start()
            }
        } catch (e: Exception) {
            text += e.message
            binding.info.text = text
            return
        }

        thread {
            val buf = ByteArray(1024)
            try {
                do {
                    val n = proc!!.inputStream.read(buf)
                    if (n > 0) {
                        text += String(buf.copyOfRange(0, n))
                        binding.info.post { binding.info.text = text }
                    }
                } while (n != -1)
            } catch (e: IOException) {}
            proc = null
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        proc?.destroy()
    }
}
