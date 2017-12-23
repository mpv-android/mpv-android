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
    protected void setupToolbar(Toolbar toolbar) {
        toolbar.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {}

    @Override
    public void onClickCheckable(View v, CheckableViewHolder vh) {
        mListener.onFilePicked(vh.file);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType != LogicHandler.VIEWTYPE_FILE)
            return super.onCreateViewHolder(parent, viewType);
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.filepicker_item, parent, false);
        return new CheckableViewHolder(v);
    }

    public boolean isBackTop() {
        return compareFiles(mCurrentPath, new File("/")) == 0;
    }
}
