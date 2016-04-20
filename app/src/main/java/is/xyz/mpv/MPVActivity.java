package is.xyz.mpv;

import android.app.Activity;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MPVActivity extends Activity implements EventObserver {
    private static final String TAG = "mpv";
    // how long should controls be displayed on screen
    private static final int CONTROLS_DISPLAY_TIMEOUT = 1000;

    MPVView player;
    View controls;

    Handler hideHandler;
    HideControlsRunnable hideControls;

    SeekBar seekbar;

    boolean userIsOperatingSeekbar = false;

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser)
                return;
            player.setTimePos(progress);
            updatePlaybackStatus();
        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) {
            userIsOperatingSeekbar = true;
        }
        @Override public void onStopTrackingTouch(SeekBar seekBar) {
            userIsOperatingSeekbar = false;
        }
    };

    @Override protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Do copyAssets here and not in MainActivity because mpv can be launched from a file browser
        copyAssets();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.player);
        controls = findViewById(R.id.controls);
        seekbar = (SeekBar) findViewById(R.id.controls_seekbar);

        controls.setVisibility(View.GONE);

        hideHandler = new Handler();
        hideControls = new HideControlsRunnable(controls, getWindow().getDecorView());
        hideControls.run();

        String filepath = null;

        Intent i = getIntent();
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            // launched as viewer for a specific file
            Uri u = i.getData();
            if (u.getScheme().equals("file"))
                filepath = u.getPath();
            else if (u.getScheme().equals("content"))
                filepath = getRealPathFromURI(u);
            else if (u.getScheme().equals("http"))
                filepath = u.toString();

            if (filepath == null) {
                Log.e(TAG, "unknown scheme: " + u.getScheme());
            }
        } else {
            filepath = i.getStringExtra("filepath");
        }

        player = (MPVView) findViewById(R.id.mpv_view);
        player.initialize(getApplicationContext().getFilesDir().getPath());
        player.addObserver(this);
        player.playFile(filepath);

        hideHandler.postDelayed(hideControls, CONTROLS_DISPLAY_TIMEOUT);
        updatePlaybackStatus();

        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        // After hiding the interface with SYSTEM_UI_FLAG_HIDE_NAVIGATION the next tap only shows the UI without
        // calling dispatchTouchEvent. Use this to showControls even in this case.
        player.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int vis) {
                if (vis == 0) {
                    showControls();
                }
            }
        });
    }

    @Override protected void onDestroy() {
        player.destroy();
        player = null;
        super.onDestroy();
    }

    private String getRealPathFromURI(Uri contentUri) {
        // http://stackoverflow.com/questions/3401579/#3414749
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getApplicationContext().getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private void copyAssets() {
        AssetManager assetManager = getApplicationContext().getAssets();
        String files[] = {"subfont.ttf"};
        String configDir = getApplicationContext().getFilesDir().getPath();
        for (String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = assetManager.open(filename, AssetManager.ACCESS_STREAMING);
                File outFile = new File(configDir + "/" + filename);
                // XXX: .available() officially returns an *estimated* number of bytes available
                // this is only accurate for generic streams, asset streams return the full file size
                if (outFile.length() == in.available()) {
                    in.close();
                    Log.w(TAG, "Skipping copy of asset file (exists same size): " + filename);
                    continue;
                }
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.close();
                Log.w(TAG, "Copied asset file: " + filename);
            } catch(IOException e) {
                Log.e(TAG, "Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[32768];
        int r;
        while ((r = in.read(buf)) != -1)
            out.write(buf, 0, r);
    }

    @Override protected void onPause() {
        player.onPause();

        super.onPause();
    }

    @Override protected void onResume() {
        player.onResume();

        super.onResume();
    }

    private void showControls() {
        controls.setVisibility(View.VISIBLE);
        hideHandler.removeCallbacks(hideControls);
        hideHandler.postDelayed(hideControls, CONTROLS_DISPLAY_TIMEOUT);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        showControls();
        return super.dispatchTouchEvent(ev);
    }

    public void playPause(View view) {
        player.cyclePause();
    }

    String prettyTime(int d) {
        int hours = d / 3600, minutes = (d % 3600) / 60, seconds = d % 60;
        if (hours == 0)
            return String.format("%02d:%02d", minutes, seconds);
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public void updatePlaybackStatus() {
        int duration = player.getDuration();
        int position = player.getTimePos();

        TextView durationView = (TextView) findViewById(R.id.controls_duration);
        durationView.setText(prettyTime(duration));
        TextView positionView = (TextView) findViewById(R.id.controls_position);
        positionView.setText(prettyTime(position));

        if (!userIsOperatingSeekbar) {
            seekbar.setMax(duration);
            seekbar.setProgress(position);
        }
    }

    @Override public void eventProperty(String property, long value) {
        switch (property) {
            case "time-pos":
                runOnUiThread(new Runnable() { public void run() { updatePlaybackStatus(); } });
                break;
        }
    }
}

class HideControlsRunnable implements Runnable {
    private View controls;
    private View decorView;

    public HideControlsRunnable(View controls_, View decorView_) {
        super();
        controls = controls_;
        decorView = decorView_;
    }

    @Override public void run() {
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        controls.setVisibility(View.GONE);

        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(flags);
    }
}
