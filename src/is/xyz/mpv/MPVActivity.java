package is.xyz.mpv;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

public class MPVActivity extends Activity {

    MPVView mView;

    @Override protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mView = new MPVView(getApplication());
        setContentView(mView);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        copyAssets();
    }

    @Override protected void onPause() {
        super.onPause();
        mView.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    private void copyAssets() {
        AssetManager assetManager = getApplicationContext().getAssets();
        String files[] = {"DroidSansFallbackFull.ttf"};
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename, AssetManager.ACCESS_STREAMING);
                File outFile = new File("/data/data/" + getApplicationContext().getPackageName() + "/" + filename);
                // XXX: .available() officially returns an *estimated* number of bytes available
                // this is only accurate for generic streams, asset streams return the full file size
                if (outFile.length() == in.available()) {
                    in.close();
                    Log.w("mpv", "Skipping copy of asset file (exists same size): " + filename);
                    continue;
                }
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.close();
                Log.w("mpv", "Copied asset file: " + filename);
            } catch(IOException e) {
                Log.e("mpv", "Failed to copy asset file: " + filename, e);
                continue;
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
