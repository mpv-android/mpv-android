package `is`.xyz.mpv

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "NotificationButtonReceiver: ${intent!!.action}")
        // remember to update AndroidManifest.xml too when adding here
        when (intent.action) {
            "$PREFIX.PLAY_PAUSE" -> MPVLib.command(arrayOf("cycle", "pause"))
            "$PREFIX.ACTION_PREV" -> MPVLib.command(arrayOf("playlist-prev"))
            "$PREFIX.ACTION_NEXT" -> MPVLib.command(arrayOf("playlist-next"))
        }
    }

    companion object {
        fun createIntent(context: Context, action: String): PendingIntent {
            val intent = Intent("$PREFIX.$action")
            // turn into explicit intent
            intent.component = ComponentName(context, NotificationButtonReceiver::class.java)
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        private const val TAG = "mpv"
        private const val PREFIX = "is.xyz.mpv"
    }
}