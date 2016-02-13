package is.xyz.mpv;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;

import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AbstractFilePickerFragment.OnFilePickedListener {
    private static final String TAG = "mpv";

    private MPVFilePickerFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.nnf_button_container).setVisibility(View.GONE);
        fragment = (MPVFilePickerFragment) getSupportFragmentManager().findFragmentById(R.id.file_picker_fragment);

        // The correct way is to modify styles.xml.
        // It doesn't work and I'm too tired to figure out why so let's have this ugly hack instead.
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setTitle(Html.fromHtml("<font color=\"#ffffff\">" + getString(R.string.mpv_activity) + "</font>"));
    }

    private void playFile(String filepath) {
        Intent i = new Intent(this, MPVActivity.class);
        i.putExtra("filepath", filepath);
        startActivity(i);
    }

    public void onFilePicked(Uri file) {
        File f = new File(file.getPath());
        playFile(f.getAbsolutePath());
    }

    public void onFilesPicked(List<Uri> files) {}

    public void onCancelled() {}

    @Override
    public void onBackPressed() {
        if (fragment.isBackTop()) {
            super.onBackPressed();
        } else {
            fragment.goUp();
        }
    }
}
