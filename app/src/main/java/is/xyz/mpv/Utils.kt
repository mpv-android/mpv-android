package `is`.xyz.mpv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import java.io.File
import kotlin.math.roundToInt

object Utils {
    fun hasSoftwareKeys(activity: Activity): Boolean {
        // Detect whether device has software home button
        // https://stackoverflow.com/questions/14853039/#answer-14871974
        val disp = activity.windowManager.defaultDisplay

        val realMetrics = DisplayMetrics()
        disp.getRealMetrics(realMetrics)
        val realW = realMetrics.widthPixels
        val realH = realMetrics.heightPixels
        val metrics = DisplayMetrics()
        disp.getMetrics(metrics)
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        return (realW - w > 0) or (realH - h > 0)
    }

    fun convertDp(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.resources.displayMetrics).toInt()
    }

    fun prettyTime(d: Int): String {
        val hours = d / 3600
        val minutes = d % 3600 / 60
        val seconds = d % 60
        if (hours == 0)
            return "%02d:%02d".format(minutes, seconds)
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun prettyTime(d: Double): String = prettyTime(d.roundToInt())

    fun getScreenBrightness(activity: Activity): Float? {
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

    data class StoragePath(val path: File, val description: String)

    @SuppressLint("NewApi")
    fun getStorageVolumes(context: Context): List<StoragePath> {
        val list = mutableListOf<StoragePath>()
        assert(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        // check all media dirs, there will be one on each storage volume
        for (path in context.externalMediaDirs) {
            val svol = storageManager.getStorageVolume(path)
            if (svol == null) {
                Log.e(TAG, "Can't get storage volume for $path")
                continue
            }
            if (svol.state != Environment.MEDIA_MOUNTED)
                continue

            // find the actual root path of that volume
            var root = path
            while (storageManager.getStorageVolume(root.parentFile) == svol) {
                root = root.parentFile
            }

            list.add(StoragePath(root, svol.getDescription(context)))
        }
        return list
    }

    fun viewGroupMove(from: ViewGroup, id: Int, to: ViewGroup, toIndex: Int) {
        val view: View? = (0 until from.childCount)
                .map { from.getChildAt(it) }.firstOrNull { it.id == id }
        if (view == null)
            error("$from does not have child with id=$id")
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
            return (0 until view.childCount).sumBy { visibleChildren(view.getChildAt(it)) }
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
}