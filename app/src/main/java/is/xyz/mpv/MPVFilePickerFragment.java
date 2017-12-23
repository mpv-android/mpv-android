package is.xyz.mpv;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import is.xyz.filepicker.FilePickerFragment;
import is.xyz.filepicker.LogicHandler;

import java.io.File;

public class MPVFilePickerFragment extends FilePickerFragment {

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {}

    @Override
    public void onClickCheckable(View v, FileViewHolder vh) {
        mListener.onFilePicked(vh.file);
    }

    public boolean isBackTop() {
        return compareFiles(mCurrentPath, new File("/")) == 0;
    }

    @Override
    public void onChangePath(File file) {
        if (file != null)
            ((MainActivity)getActivity()).getSupportActionBar().setTitle("mpv :: " + file.getPath());
    }
}
