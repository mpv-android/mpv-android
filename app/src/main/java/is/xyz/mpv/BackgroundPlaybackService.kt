package `is`.xyz.mpv

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle

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
    private var paused: Boolean = false
    private var shouldShowPrevNext: Boolean = false

    private fun buildNotificationAction(@DrawableRes icon: Int, @StringRes title: Int, intentAction: String): NotificationCompat.Action {
        val intent = NotificationButtonReceiver.createIntent(this, intentAction)

        val builder = NotificationCompat.Action.Builder(icon, getString(title), intent)
        with (builder) {
            setContextual(false)
            setShowsUserInterface(false)
            return build()
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MPVActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        else
            PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        with (builder) {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setContentTitle(cachedMetadata.formatTitle())
            setContentText(cachedMetadata.formatArtistAlbum())
            setSmallIcon(R.drawable.ic_mpv_symbolic)
            setContentIntent(pendingIntent)
            setOngoing(true)
        }

        thumbnail?.let {
            builder.setLargeIcon(it)

            builder.setColorized(true)
            // scale thumbnail to a single color in two steps
            val b1 = Bitmap.createScaledBitmap(it, 16, 16, true)
            val b2 = Bitmap.createScaledBitmap(b1, 1, 1, true)
            builder.setColor(b2.getPixel(0, 0))
            b2.recycle(); b1.recycle()
        }

        val playPauseAction = if (paused)
            buildNotificationAction(R.drawable.ic_play_arrow_black_24dp, R.string.btn_play, "PLAY_PAUSE")
        else
            buildNotificationAction(R.drawable.ic_pause_black_24dp, R.string.btn_pause, "PLAY_PAUSE")

        if (shouldShowPrevNext) {
            builder.addAction(buildNotificationAction(
                R.drawable.ic_skip_previous_black_24dp, R.string.dialog_prev, "ACTION_PREV"
            ))
            builder.addAction(playPauseAction)
            builder.addAction(buildNotificationAction(
                R.drawable.ic_skip_next_black_24dp, R.string.dialog_next, "ACTION_NEXT"
            ))
            builder.setStyle(MediaStyle().setShowActionsInCompactView(0, 2))
        } else {
            builder.addAction(playPauseAction)
            builder.setStyle(MediaStyle())
        }

        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "BackgroundPlaybackService: starting")

        // read some metadata

        cachedMetadata.readAll()
        paused = MPVLib.getPropertyBoolean("pause")
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

    override fun eventProperty(property: String, value: Boolean) {
        if (property != "pause")
            return
        paused = value

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

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
            val manager = NotificationManagerCompat.from(context)
            val builder = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_MIN)
            manager.createNotificationChannel(with (builder) {
                setName(context.getString(R.string.pref_background_play_title))
                build()
            })
        }

        private const val TAG = "mpv"
    }
}
