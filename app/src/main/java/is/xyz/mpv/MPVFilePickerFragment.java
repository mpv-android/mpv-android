package is.xyz.mpv;

import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import is.xyz.filepicker.FilePickerFragment;

import java.io.File;

public class MPVFilePickerFragment extends FilePickerFragment {

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {}

    @Override
    public void onClickCheckable(@NonNull View v, @NonNull FileViewHolder vh) {
        mListener.onFilePicked(vh.file);
    }

    public boolean isBackTop() {
        return compareFiles(mCurrentPath, new File("/")) == 0;
    }

    @Override
    public void onChangePath(File file) {
        ActionBar bar = ((MainActivity)getActivity()).getSupportActionBar();
        if (file != null && bar != null)
            bar.setTitle("mpv :: " + file.getPath());
    }
}
