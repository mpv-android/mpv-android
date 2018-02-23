package `is`.xyz.mpv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "NotificationButtonReceiver: ${intent!!.action}")
        when(intent!!.action) {
            "$PREFIX.ACTION_PREV" -> MPVLib.command(arrayOf("playlist-prev"))
            "$PREFIX.ACTION_NEXT" -> MPVLib.command(arrayOf("playlist-next"))
        }
    }

    companion object {
        private val TAG = "mpv"
        private val PREFIX = "is.xyz.mpv"
    }
}