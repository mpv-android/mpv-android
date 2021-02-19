package `is`.xyz.mpv

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.util.Log

/*
    All this service does is
    - Discourage Android from killing mpv while it's in background
    - Update the persistent notification (which we're forced to display)
*/

class BackgroundPlaybackService : Service(), MPVLib.EventObserver {
    override fun onCreate() {
        MPVLib.addObserver(this)
    }

    private var cachedMetadata = Utils.AudioMetadata()
    private var shouldShowPrevNext: Boolean = false

    @Suppress("DEPRECATION") // deliberate to support lower API levels
    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MPVActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        else
            Notification.Builder(this)

        builder
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(cachedMetadata.formatTitle())
                .setContentText(cachedMetadata.formatArtistAlbum())
                .setSmallIcon(R.drawable.ic_mpv_symbolic)
                .setContentIntent(pendingIntent)

        thumbnail?.let {
            builder.setLargeIcon(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setColorized(true)
                // scale thumbnail to a single color in two steps
                val b1 = Bitmap.createScaledBitmap(it, 16, 16, true)
                val b2 = Bitmap.createScaledBitmap(b1, 1, 1, true)
                builder.setColor(b2.getPixel(0, 0))
                b2.recycle(); b1.recycle()
            }
        }
        if (shouldShowPrevNext) {
            // action icons need to be 32dp according to the docs
            builder.addAction(R.drawable.ic_skip_previous_black_32dp, "Prev",
                    NotificationButtonReceiver.createIntent(this, "ACTION_PREV"))
            builder.addAction(R.drawable.ic_skip_next_black_32dp, "Next",
                    NotificationButtonReceiver.createIntent(this, "ACTION_NEXT"))
            builder.style = Notification.MediaStyle().setShowActionsInCompactView(0, 1)
        } else {
            builder.style = Notification.MediaStyle()
        }

        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "BackgroundPlaybackService: starting")

        // read some metadata

        cachedMetadata.readAll()
        shouldShowPrevNext = MPVLib.getPropertyInt("playlist-count") ?: 0 > 1

        // create notification and turn this into a "foreground service"

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY // Android can't restart this service on its own
    }

    override fun onDestroy() {
        MPVLib.removeObserver(this)

        Log.v(TAG, "BackgroundPlaybackService: destroyed")
    }

    override fun onBind(intent: Intent): IBinder? { return null }

    /* Event observers */

    override fun eventProperty(property: String) { }

    override fun eventProperty(property: String, value: Boolean) { }

    override fun eventProperty(property: String, value: Long) { }

    override fun eventProperty(property: String, value: String) {
        if (!cachedMetadata.update(property, value))
            return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun event(eventId: Int) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE)
            stopSelf()
    }


    companion object {
        /* Using this property MPVActivity gives us a thumbnail
           to display alongside the permanent notification */
        var thumbnail: Bitmap? = null

        private const val NOTIFICATION_ID = 12345
        private const val NOTIFICATION_CHANNEL_ID = "background_playback"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.pref_background_play_title)
                val channel = NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        name, NotificationManager.IMPORTANCE_MIN)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        private const val TAG = "mpv"
    }
}