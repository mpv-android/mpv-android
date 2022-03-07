package `is`.xyz.mpv

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import kotlin.math.abs

object Utils {
    fun copyAssets(context: Context) {
        val assetManager = context.assets
        val files = arrayOf("subfont.ttf", "cacert.pem")
        val configDir = context.filesDir.path
        for (filename in files) {
            var ins: InputStream? = null
            var out: OutputStream? = null
            try {
                ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
                val outFile = File("$configDir/$filename")
                // Note that .available() officially returns an *estimated* number of bytes available
                // this is only true for generic streams, asset streams return the full file size
                if (outFile.length() == ins.available().toLong()) {
                    Log.v(TAG, "Skipping copy of asset file (exists same size): $filename")
                    continue
                }
                out = FileOutputStream(outFile)
                ins.copyTo(out)
                Log.w(TAG, "Copied asset file: $filename")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy asset file: $filename", e)
            } finally {
                ins?.close()
                out?.close()
            }
        }
    }

    fun convertDp(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.resources.displayMetrics
        ).toInt()
    }

    fun prettyTime(d: Int, sign: Boolean = false): String {
        if (sign)
            return (if (d >= 0) "+" else "-") + prettyTime(abs(d))

        val hours = d / 3600
        val minutes = d % 3600 / 60
        val seconds = d % 60
        if (hours == 0)
            return "%02d:%02d".format(minutes, seconds)
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun getScreenBrightness(activity: AppCompatActivity): Float? {
        // check if window has brightness set
        val lp = activity.window.attributes
        if (lp.screenBrightness >= 0f)
            return lp.screenBrightness

        // read system pref: https://stackoverflow.com/questions/4544967/#answer-8114307
        // (doesn't work with auto-brightness mode)
        val resolver = activity.contentResolver
        return try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Settings.SettingNotFoundException) {
            null
        }
    }

    fun viewGroupMove(from: ViewGroup, id: Int, to: ViewGroup, toIndex: Int) {
        val view: View = (0 until from.childCount)
            .map { from.getChildAt(it) }.firstOrNull { it.id == id }
            ?: error("$from does not have child with id=$id")
        from.removeView(view)
        to.addView(view, if (toIndex >= 0) toIndex else (to.childCount + 1 + toIndex))
    }

    fun viewGroupReorder(group: ViewGroup, idOrder: Array<Int>) {
        val m = mutableMapOf<Int, View>()
        for (i in 0 until group.childCount) {
            val c = group.getChildAt(i)
            m[c.id] = c
        }
        group.removeAllViews()
        // Readd children in specified order and unhide
        for (id in idOrder) {
            val c = m.remove(id) ?: error("$group did not have child with id=$id")
            c.visibility = View.VISIBLE
            group.addView(c)
        }
        // Keep unspecified children but hide them
        for (c in m.values) {
            c.visibility = View.GONE
            group.addView(c)
        }
    }

    fun fileBasename(str: String): String {
        val isURL = str.indexOf("://") != -1
        val last = str.replaceBeforeLast('/', "").trimStart('/')
        return if (isURL)
            Uri.decode(last.replaceAfter('?', "").trimEnd('?'))
        else
            last
    }

    fun visibleChildren(view: View): Int {
        if (view is ViewGroup && view.visibility == View.VISIBLE) {
            return (0 until view.childCount).sumOf { visibleChildren(view.getChildAt(it)) }
        }
        return if (view.visibility == View.VISIBLE) 1 else 0
    }

    class AudioMetadata {
        var mediaTitle: String? = null
        var mediaArtist: String? = null
        var mediaAlbum: String? = null

        fun readAll() {
            mediaTitle = MPVLib.getPropertyString("media-title")
            mediaArtist = MPVLib.getPropertyString("metadata/by-key/Artist")
            mediaAlbum = MPVLib.getPropertyString("metadata/by-key/Album")
        }

        fun update(property: String, value: String): Boolean {
            when (property) {
                "media-title" -> mediaTitle = value
                "metadata/by-key/Artist" -> mediaArtist = value
                "metadata/by-key/Album" -> mediaAlbum = value
                else -> return false
            }
            return true
        }

        fun formatTitle(): String? = if (!mediaTitle.isNullOrEmpty()) mediaTitle else null

        fun formatArtistAlbum(): String? {
            val artistEmpty = mediaArtist.isNullOrEmpty()
            val albumEmpty = mediaAlbum.isNullOrEmpty()
            return when {
                !artistEmpty && !albumEmpty -> "$mediaArtist / $mediaAlbum"
                !artistEmpty -> mediaAlbum
                !albumEmpty -> mediaArtist
                else -> null
            }
        }
    }

    private const val TAG = "mpv"

    // This is used to filter files in the file picker, so it contains just about everything
    // FFmpeg/mpv could possibly read
    val MEDIA_EXTENSIONS = arrayListOf(
        /* Playlist */
        "cue", "m3u", "m3u8", "pls", "vlc",

        /* Audio */
        "3ga", "3ga2", "a52", "aac", "ac3", "adt", "adts", "aif", "aifc", "aiff", "alac",
        "amr", "ape", "au", "awb", "dts", "dts-hd", "dtshd", "eac3", "f4a", "flac", "lpcm",
        "m1a", "m2a", "m4a", "mk3d", "mka", "mlp", "mp+", "mp1", "mp2", "mp3", "mpa", "mpc",
        "mpga", "mpp", "oga", "ogg", "opus", "pcm", "ra", "ram", "rax", "shn", "snd", "spx",
        "tak", "thd", "thd+ac3", "true-hd", "truehd", "tta", "wav", "weba", "wma", "wv",
        "wvp",

        /* Video / Container */
        "264", "265", "3g2", "3ga", "3gp", "3gp2", "3gpp", "3gpp2", "3iv", "amr", "asf",
        "asx", "av1", "avc", "avf", "avi", "bdm", "bdmv", "clpi", "cpi", "divx", "dv", "evo",
        "evob", "f4v", "flc", "fli", "flic", "flv", "gxf", "h264", "h265", "hdmov", "hdv",
        "hevc", "lrv", "m1u", "m1v", "m2t", "m2ts", "m2v", "m4u", "m4v", "mkv", "mod", "moov",
        "mov", "mp2", "mp2v", "mp4", "mp4v", "mpe", "mpeg", "mpeg2", "mpeg4", "mpg", "mpg4",
        "mpl", "mpls", "mpv", "mpv2", "mts", "mtv", "mxf", "mxu", "nsv", "nut", "ogg", "ogm",
        "ogv", "ogx", "qt", "qtvr", "rm", "rmj", "rmm", "rms", "rmvb", "rmx", "rv", "rvx",
        "sdp", "tod", "trp", "ts", "tsa", "tsv", "tts", "vc1", "vfw", "vob", "vro", "webm",
        "wm", "wmv", "wmx", "x264", "x265", "xvid", "y4m", "yuv",

        /* Picture */
        "apng", "bmp", "exr", "gif", "j2c", "j2k", "jfif", "jp2", "jpc", "jpe", "jpeg", "jpg",
        "jpg2", "png", "tga", "tif", "tiff", "webp",
    )
}
