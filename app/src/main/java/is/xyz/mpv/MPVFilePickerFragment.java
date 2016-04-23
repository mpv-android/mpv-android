package is.xyz.mpv;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nononsenseapps.filepicker.FilePickerFragment;
import com.nononsenseapps.filepicker.LogicHandler;

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
        if (!allowMultiple) {
            // Clear is necessary, in case user clicked some checkbox directly
            mCheckedItems.clear();
            mCheckedItems.add(vh.file);
            onClickOk(null);
        } else {
            super.onClickCheckable(v, vh);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType != LogicHandler.VIEWTYPE_CHECKABLE)
            return super.onCreateViewHolder(parent, viewType);
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.filepicker_item, parent, false);
        return new CheckableViewHolder(v);
    }

    /**
     * For consistency, the top level the back button checks against should be the start path.
     * But it will fall back on /.
     */
    public File getBackTop() {
        if (getArguments() != null && getArguments().containsKey(KEY_START_PATH)) {
            return getPath(getArguments().getString(KEY_START_PATH));
        } else {
            return new File("/");
        }
    }

    public boolean isBackTop() {
        if (mCurrentPath != null) {
            return (compareFiles(mCurrentPath, getBackTop()) == 0) || (compareFiles(mCurrentPath, new File("/")) == 0);
        } else {
            return false;
        }
    }
}
