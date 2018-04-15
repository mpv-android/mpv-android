package `is`.xyz.mpv

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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

class BackgroundPlaybackService : Service(), EventObserver {
    override fun onCreate() {
        MPVLib.addObserver(this)
        MPVLib.observeProperty("media-title", MPVLib.mpvFormat.MPV_FORMAT_STRING)
        MPVLib.observeProperty("metadata/by-key/Artist", MPVLib.mpvFormat.MPV_FORMAT_STRING)
        MPVLib.observeProperty("metadata/by-key/Album", MPVLib.mpvFormat.MPV_FORMAT_STRING)
    }

    private var cachedMediaTitle: String? = null
    private var cachedMediaArtist: String? = null
    private var cachedMediaAlbum: String? = null
    private var shouldShowPrevNext: Boolean = false

    private fun createButtonIntent(action: String): PendingIntent {
        val intent = Intent()
        intent.action = "is.xyz.mpv.$action"
        return PendingIntent.getBroadcast(this, 0, intent, 0)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MPVActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        else
            Notification.Builder(this)

        builder
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(cachedMediaTitle)
                .setSmallIcon(R.drawable.ic_mpv_symbolic)
                .setContentIntent(pendingIntent)
        if (thumbnail != null)
            builder.setLargeIcon(thumbnail)
        if (!cachedMediaArtist.isNullOrEmpty() && !cachedMediaAlbum.isNullOrEmpty())
            builder.setContentText("$cachedMediaArtist / $cachedMediaAlbum")
        else if (!cachedMediaArtist.isNullOrEmpty())
            builder.setContentText(cachedMediaAlbum)
        else if (!cachedMediaAlbum.isNullOrEmpty())
            builder.setContentText(cachedMediaArtist)
        if (shouldShowPrevNext) {
            // action icons need to be 32dp according to the docs
            builder.addAction(R.drawable.ic_skip_previous_black_32dp, "Prev", createButtonIntent("ACTION_PREV"))
            builder.addAction(R.drawable.ic_skip_next_black_32dp, "Next", createButtonIntent("ACTION_NEXT"))
            builder.setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1))
        }

        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "BackgroundPlaybackService: starting")

        // read some metadata

        cachedMediaTitle = MPVLib.getPropertyString("media-title")
        cachedMediaArtist = MPVLib.getPropertyString("metadata/by-key/Artist")
        cachedMediaAlbum = MPVLib.getPropertyString("metadata/by-key/Album")
        shouldShowPrevNext = MPVLib.getPropertyInt("playlist-count") ?: 0 > 1

        // create notification and turn this into a "foreground service"

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // resume playback (audio-only)

        MPVLib.setPropertyString("vid", "no")
        MPVLib.setPropertyBoolean("pause", false)

        return Service.START_NOT_STICKY // Android can't restart this service on its own
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
        when (property) {
            "media-title" -> cachedMediaTitle = value
            "metadata/by-key/Artist" -> cachedMediaArtist = value
            "metadata/by-key/Album" -> cachedMediaAlbum = value
            else -> return
        }

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

        private val NOTIFICATION_ID = 12345 // TODO: put this into resource file
        val NOTIFICATION_CHANNEL_ID = "background_playback"
        private val TAG = "mpv"
    }
}