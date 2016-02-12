package is.xyz.mpv;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.net.Uri;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import com.nononsenseapps.filepicker.FilePickerActivity;

public class MPVActivity extends Activity {
    private static final String TAG = "mpv";
    private static final int FILE_CODE = 0;
    // how long should controls be displayed on screen
    private static final int CONTROLS_DISPLAY_TIMEOUT = 1000;

    private String configDir;

    MPVView mView;
    View controls;

    Handler hideHandler;
    HideControlsRunnable hideControls;

    @Override protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.player);
        mView = (MPVView) findViewById(R.id.mpv_view);

        controls = findViewById(R.id.controls);
        controls.setVisibility(View.GONE);

        hideHandler = new Handler();
        hideControls = new HideControlsRunnable(controls, getWindow().getDecorView());
        hideControls.run();

        if(getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            // launched from application menu
            Intent i = new Intent(this, FilePickerActivity.class);
            // Specify initial directory as external storage
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
            startActivityForResult(i, FILE_CODE);
        } else if(getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            // launched as viewer for a specific file
            Uri u = getIntent().getData();
            String filepath = null;
            if (u.getScheme().equals("file"))
                filepath = u.getPath();
            else if (u.getScheme().equals("content"))
                filepath = getRealPathFromURI(u);
            else if (u.getScheme().equals("http"))
                filepath = u.toString();
            if (filepath == null) {
                Log.e(TAG, "unknown scheme: " + u.getScheme());
                return;
            }
            MPVLib.command(new String[] {"loadfile", filepath});
        } else {
            Log.e(TAG, "launched with unrecognized intent: " + getIntent().getAction());
            return;
        }

        configDir = getApplicationContext().getFilesDir().getPath(); // usually /data/data/is.xyz.mpv/files
        MPVLib.setconfigdir(configDir);

        copyAssets();

        hideHandler.postDelayed(hideControls, CONTROLS_DISPLAY_TIMEOUT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            File f = new File(data.getData().getPath());
            MPVLib.command(new String[] {"loadfile", f.getAbsolutePath()});
        } else if (requestCode == FILE_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        mView.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        controls.setVisibility(View.VISIBLE);
        hideHandler.removeCallbacks(hideControls);
        hideHandler.postDelayed(hideControls, CONTROLS_DISPLAY_TIMEOUT);
        return super.dispatchTouchEvent(ev);
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
