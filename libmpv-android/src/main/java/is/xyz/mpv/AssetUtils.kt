package `is`.xyz.mpv

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.IOException

/** Asset setup used by applications embedding [MPVLib]. */
object Utils {
    private const val TAG = "mpv"

    fun copyAssets(context: Context) {
        copyAssetFile(context.assets, "cacert.pem", File(context.filesDir, "cacert.pem"))
        writeFontsConf(context, File(context.filesDir, "fonts.conf"))
    }

    private fun copyAssetFile(assetManager: AssetManager, filename: String, outFile: File) {
        try {
            assetManager.open(filename, AssetManager.ACCESS_STREAMING).use { input ->
                val assetSize = input.available().toLong()
                if (outFile.length() == assetSize) {
                    Log.v(TAG, "Skipping copy of asset file (exists same size): $filename")
                    return
                }
                outFile.outputStream().use { output -> input.copyTo(output) }
                Log.w(TAG, "Copied asset file ($assetSize bytes): $filename")
            }
        } catch (error: IOException) {
            Log.e(TAG, "Failed to copy asset file: $filename", error)
        }
    }

    private fun writeFontsConf(context: Context, configFile: File) {
        val contents = """
            <fontconfig>
            <dir>/system/fonts/</dir>
            <dir>/product/fonts/</dir>
            <cachedir>${context.cacheDir.path}</cachedir>
            <alias><family>serif</family><prefer><family>Noto Serif</family></prefer></alias>
            <alias><family>sans-serif</family><prefer><family>Roboto</family><family>Noto Sans</family></prefer></alias>
            <alias><family>monospace</family><prefer><family>Droid Sans Mono</family></prefer></alias>
            </fontconfig>
        """.trimIndent()

        try {
            configFile.writeText(contents)
        } catch (error: IOException) {
            Log.w(TAG, "Failed to write fonts.conf", error)
        }
    }
}
