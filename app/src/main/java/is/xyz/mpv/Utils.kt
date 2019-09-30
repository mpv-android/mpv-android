package `is`.xyz.mpv

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue

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
}