package is.xyz.mpv;

import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

import is.xyz.filepicker.DocumentPickerFragment;

public class MPVDocumentPickerFragment extends DocumentPickerFragment {

    public boolean naturalSort = false;

    public MPVDocumentPickerFragment(@NonNull Uri root) {
        super(root);
    }

    @Override
    protected int compareDocuments(@NonNull Document lhs, @NonNull Document rhs) {
        if (lhs.isDirectory() != rhs.isDirectory())
            return rhs.isDirectory() ? 1 : -1;
        if (naturalSort)
            return MPVFilePickerFragment.naturalCompare(lhs.getDisplayName(), rhs.getDisplayName());
        return lhs.compareTo(rhs);
    }

    @Override
    public void onClickCheckable(@NonNull View view, @NonNull FileViewHolder vh) {
        mListener.onDocumentPicked(vh.file, false);
    }

    @Override
    public boolean onLongClickCheckable(@NonNull View view, @NonNull DirViewHolder vh) {
        mListener.onDocumentPicked(vh.file, true);
        return true;
    }

    public boolean isBackTop() {
        return mCurrentPath.equals(getRoot());
    }
}
