package is.xyz.filepicker;

import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

public class MPVDocumentPickerFragment extends DocumentPickerFragment {

    public MPVDocumentPickerFragment(@NonNull Uri root) {
        super(root);
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
