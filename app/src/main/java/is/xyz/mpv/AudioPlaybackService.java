package is.xyz.mpv;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

public class AudioPlaybackService extends Service implements EventObserver {
    @Override
    public void onCreate() {
        Log.v(TAG, "AudioPlaybackService created");
        MPVLib.addObserver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "AudioPlaybackService starting");

        // create notification and turn this into a foreground service

        Intent notificationIntent = new Intent(this, MPVActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setContentTitle("mpv")
                        .setContentText("foobar1")
                        .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                        .setContentIntent(pendingIntent)
                        .setTicker("foobar2")
                        .build();
        startForeground(12345, notification);

        // resume playback (of audio)

        MPVLib.setPropertyString("vid", "no");
        MPVLib.setPropertyBoolean("pause", false);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        MPVLib.setPropertyBoolean("pause", true);
        MPVLib.removeObserver(this);

        Log.v(TAG, "AudioPlaybackService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* Event observers */

    @Override
    public void eventProperty(@NotNull String property) {}

    @Override
    public void eventProperty(@NotNull String property, long value) {}

    @Override
    public void eventProperty(@NotNull String property, boolean value) {}

    @Override
    public void eventProperty(@NotNull String property, @NotNull String value) {}

    @Override
    public void event(int eventId) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE)
            stopSelf();
    }

    private static final String TAG = "mpv";
}
