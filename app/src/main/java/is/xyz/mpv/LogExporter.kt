package `is`.xyz.mpv

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.IOException

object LogExporter {
    private const val LOGCAT = "/system/bin/logcat"

    /**
     * Writes device info and this app's logcat buffer to a file under the app cache directory.
     */
    @Throws(IOException::class)
    fun createLogFile(context: Context): File {
        val text = buildString {
            append("mpv-android ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("\n--- logcat (this app) ---\n")
            append(captureLogcat())
        }
        val file = File(context.cacheDir, "mpv-log-${System.currentTimeMillis()}.txt")
        file.writeText(text)
        return file
    }

    private fun captureLogcat(): String {
        val cmd = ArrayList<String>(8).apply {
            add(LOGCAT)
            add("-d")
            add("-v")
            add("threadtime")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add("--uid")
                add(Process.myUid().toString())
            } else {
                add("--pid")
                add(Process.myPid().toString())
            }
        }
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        return if (exit != 0) {
            "logcat failed (exit $exit)\n$output"
        } else {
            output.ifBlank {
                "(empty log buffer — reproduce the issue in mpv, then export again)\n"
            }
        }
    }
}
