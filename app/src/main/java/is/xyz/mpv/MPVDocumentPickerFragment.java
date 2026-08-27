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

    /**
     * Natural sort comparator: numeric substrings are compared by value rather than
     * lexicographically, so "2.m4a" comes before "10.m4a".
     * Leading zeros are ignored for value comparison ("01" == "1").
     */
    private static int naturalCompare(@NonNull String a, @NonNull String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                // find the full extent of both digit runs
                int endI = i, endJ = j;
                while (endI < a.length() && Character.isDigit(a.charAt(endI))) endI++;
                while (endJ < b.length() && Character.isDigit(b.charAt(endJ))) endJ++;
                // skip leading zeros to reach significant digits
                int sigI = i, sigJ = j;
                while (sigI < endI && a.charAt(sigI) == '0') sigI++;
                while (sigJ < endJ && b.charAt(sigJ) == '0') sigJ++;
                // longer significant run = larger number
                int lenA = endI - sigI, lenB = endJ - sigJ;
                if (lenA != lenB) return lenA - lenB;
                // same significant length: compare digit by digit
                for (int k = 0; k < lenA; k++) {
                    int cmp = a.charAt(sigI + k) - b.charAt(sigJ + k);
                    if (cmp != 0) return cmp;
                }
                // numbers equal in value; advance past entire digit run in both
                i = endI;
                j = endJ;
            } else {
                int cmp = Character.toLowerCase(ca) - Character.toLowerCase(cb);
                if (cmp != 0) return cmp;
                i++;
                j++;
            }
        }
        // compare remaining (unconsumed) character counts, not total lengths
        return (a.length() - i) - (b.length() - j);
    }

    @Override
    protected int compareDocuments(@NonNull Document lhs, @NonNull Document rhs) {
        if (lhs.isDirectory() != rhs.isDirectory())
            return rhs.isDirectory() ? 1 : -1;
        if (naturalSort)
            return naturalCompare(lhs.getDisplayName(), rhs.getDisplayName());
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
