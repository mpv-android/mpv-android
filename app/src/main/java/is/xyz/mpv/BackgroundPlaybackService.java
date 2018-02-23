package is.xyz.mpv;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

/*
    All this service does is
    - Discourage Android from killing mpv while it's in background
    - Update the persistent notification (which we're forced to display)
 */

public class BackgroundPlaybackService extends Service implements EventObserver {
    @Override
    public void onCreate() {
        MPVLib.addObserver(this);
        MPVLib.observeProperty("media-title", MPVLib.mpvFormat.MPV_FORMAT_STRING);
        MPVLib.observeProperty("metadata/by-key/Artist", MPVLib.mpvFormat.MPV_FORMAT_STRING);
        MPVLib.observeProperty("metadata/by-key/Album", MPVLib.mpvFormat.MPV_FORMAT_STRING);
    }

    private String cachedMediaTitle;
    private String cachedMediaArtist;
    private String cachedMediaAlbum;

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MPVActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder =
            new Notification.Builder(this)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentTitle(cachedMediaTitle)
                    .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                    .setContentIntent(pendingIntent);
        if (thumbnail != null)
            builder.setLargeIcon(thumbnail);
        if (!isNullOrEmpty(cachedMediaAlbum) && !isNullOrEmpty(cachedMediaAlbum))
            builder.setContentText(cachedMediaArtist + " / " + cachedMediaAlbum);
        else if (!isNullOrEmpty(cachedMediaArtist))
            builder.setContentText(cachedMediaAlbum);
        else if (!isNullOrEmpty(cachedMediaAlbum))
            builder.setContentText(cachedMediaArtist);

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "BackgroundPlaybackService: starting");

        // read some metadata

        cachedMediaTitle = MPVLib.getPropertyString("media-title");
        cachedMediaArtist = MPVLib.getPropertyString("metadata/by-key/Artist");
        cachedMediaAlbum = MPVLib.getPropertyString("metadata/by-key/Album");

        // create notification and turn this into a "foreground service"

        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        // resume playback (audio-only)

        MPVLib.setPropertyString("vid", "no");
        MPVLib.setPropertyBoolean("pause", false);

        return START_NOT_STICKY; // Android can't restart this service on its own
    }

    @Override
    public void onDestroy() {
        MPVLib.removeObserver(this);

        Log.v(TAG, "BackgroundPlaybackService: destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    /* This is called by MPVActivity to give us a thumbnail to display
       alongside the permanent notification */
    private static Bitmap thumbnail = null;
    public static void setThumbnail(Bitmap b) {
        thumbnail = b;
    }

    /* Event observers */

    @Override
    public void eventProperty(@NotNull String property) {}

    @Override
    public void eventProperty(@NotNull String property, long value) {}

    @Override
    public void eventProperty(@NotNull String property, boolean value) {}

    @Override
    public void eventProperty(@NotNull String property, @NotNull String value) {
        if (property.equals("media-title"))
            cachedMediaTitle = value;
        else if (property.equals("metadata/by-key/Artist"))
            cachedMediaArtist = value;
        else if (property.equals("metadata/by-key/Album"))
            cachedMediaAlbum = value;
        else
            return;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.notify(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public void event(int eventId) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE)
            stopSelf();
    }

    private static final int NOTIFICATION_ID = 12345; // TODO: put this into resource file
    private static final String TAG = "mpv";
}
