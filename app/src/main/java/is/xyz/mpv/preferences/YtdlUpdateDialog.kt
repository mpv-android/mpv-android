package `is`.xyz.mpv.preferences

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.databinding.YtdlUpdatePrefBinding
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class YtdlUpdatePreference(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private lateinit var binding: YtdlUpdatePrefBinding
    private var proc: Process? = null

    init {
        isPersistent = false
    }

    override fun onClick() {
        super.onClick()
        val dialogBuilder = AlertDialog.Builder(context)
        binding = YtdlUpdatePrefBinding.inflate(LayoutInflater.from(context))
        dialogBuilder.setView(binding.root)
        dialogBuilder.setTitle(title)
        dialogBuilder.setNegativeButton(android.R.string.cancel) { _: android.content.DialogInterface, _: Int ->
            proc?.destroy()
        }
        dialogBuilder.create().show()

        setupViews()
    }

    private fun setupViews() {
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

    private fun runProcess(initText: String, command: MutableList<String>) {
        if (proc != null) return

        var text = initText + "\n"
        binding.info.text = text

        command[0] = "${context.filesDir.path}/${command[0]}"
        try {
            with(ProcessBuilder(command)) {
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
}
