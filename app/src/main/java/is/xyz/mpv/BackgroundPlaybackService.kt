package `is`.xyz.mpv

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import `is`.xyz.mpv.MPVLib.MpvEvent

/*
    All this service does is
    - Discourage Android from killing mpv while it's in background
    - Update the persistent notification (which we're forced to display)
*/

class BackgroundPlaybackService : Service(), MPVLib.EventObserver {
    override fun onCreate() {
        MPVLib.addObserver(this)
    }

    private lateinit var thumbnailHandler: Handler
    private val thumbnailRunnable = Runnable {
        grabThumbnail()
        thumbnailChanged?.let {
            it() // FIXME: this is a dumb hack, we need to refactor the responsiblities
        }
        refreshNotification()
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

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MPVActivity::class.java)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val pendingIntent = PendingIntentCompat.getActivity(this, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT, false)

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

        // With an active media session, the media style will override everything
        // (including the thumbnail) and we can skip doing this.
        if (mediaToken == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            thumbnail?.let {
                builder.setLargeIcon(it)

                builder.setColorized(true)
                // scale thumbnail to a single color in two steps
                val b1 = Bitmap.createScaledBitmap(it, 16, 16, true)
                val b2 = Bitmap.createScaledBitmap(b1, 1, 1, true)
                builder.setColor(b2.getPixel(0, 0))
                b2.recycle(); b1.recycle()
            }
        }

        val playPauseAction = if (paused)
            buildNotificationAction(R.drawable.ic_play_arrow_black_24dp, R.string.btn_play, "PLAY_PAUSE")
        else
            buildNotificationAction(R.drawable.ic_pause_black_24dp, R.string.btn_pause, "PLAY_PAUSE")

        val style = MediaStyle()
        mediaToken?.let { style.setMediaSession(it) }
        if (shouldShowPrevNext) {
            builder.addAction(buildNotificationAction(
                R.drawable.ic_skip_previous_black_24dp, R.string.dialog_prev, "ACTION_PREV"
            ))
            builder.addAction(playPauseAction)
            builder.addAction(buildNotificationAction(
                R.drawable.ic_skip_next_black_24dp, R.string.dialog_next, "ACTION_NEXT"
            ))
            style.setShowActionsInCompactView(0, 1, 2) // all
        } else {
            builder.addAction(playPauseAction)
        }
        builder.setStyle(style)

        return builder.build()
    }

    @SuppressLint("NotificationPermission") // not required for foreground service
    private fun refreshNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "BackgroundPlaybackService: starting")

        thumbnailHandler = Handler(mainLooper)

        cachedMetadata.readAll()
        paused = MPVLib.getPropertyBoolean("pause") == true
        shouldShowPrevNext = (MPVLib.getPropertyInt("playlist-count") ?: 0) > 1

        // create notification and turn this into a "foreground service"

        val notification = buildNotification()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)

        return START_NOT_STICKY // Android can't restart this service on its own
    }

    override fun onDestroy() {
        MPVLib.removeObserver(this)

        thumbnailHandler.removeCallbacksAndMessages(null)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        Log.v(TAG, "BackgroundPlaybackService: destroyed")
    }

    override fun onBind(intent: Intent): IBinder? { return null }

    /* Event observers */

    override fun eventProperty(property: String) {
        if (!cachedMetadata.update(property))
            return
        refreshNotification()
    }

    override fun eventProperty(property: String, value: Boolean) {
        if (property != "pause")
            return
        paused = value
        refreshNotification()
    }

    override fun eventProperty(property: String, value: Long) { }

    override fun eventProperty(property: String, value: Double) { }

    override fun eventProperty(property: String, value: String) {
        if (!cachedMetadata.update(property, value))
            return
        refreshNotification()
    }

    override fun event(eventId: Int) {
        if (eventId == MpvEvent.MPV_EVENT_SHUTDOWN) {
            stopSelf()
        } else if (eventId == MpvEvent.MPV_EVENT_VIDEO_RECONFIG) {
            // ensure it doesn't run too often
            thumbnailHandler.removeCallbacks(thumbnailRunnable)
            thumbnailHandler.postDelayed(thumbnailRunnable, 150L)
        }
    }


    companion object {
        /* thumbnail to display alongside the permanent notification */
        var thumbnail: Bitmap? = null
        /* Set by MPVActivity; for connecting the notification to the media session */
        var mediaToken: MediaSessionCompat.Token? = null
        /* Set by MPVActivity; to notify on thumbnail changes */
        var thumbnailChanged: (() -> Unit)? = null

        private const val NOTIFICATION_ID = 12345
        private const val NOTIFICATION_CHANNEL_ID = "background_playback"
        // resolution (px) of the thumbnail
        private const val THUMB_SIZE = 384

        fun createNotificationChannel(context: Context) {
            val manager = NotificationManagerCompat.from(context)
            val builder = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_MIN)
            manager.createNotificationChannel(with (builder) {
                setName(context.getString(R.string.pref_background_play_title))
                build()
            })
        }

        fun grabThumbnail() {
            val fmt = MPVLib.getPropertyString("video-format")
            thumbnail = if (fmt.isNullOrEmpty())
                null
            else
                MPVLib.grabThumbnail(THUMB_SIZE)
        }

        private const val TAG = "mpv"
    }
}
