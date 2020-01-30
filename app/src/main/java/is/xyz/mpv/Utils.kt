package `is`.xyz.mpv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import java.io.File

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

    private val TAG = "mpv"
}