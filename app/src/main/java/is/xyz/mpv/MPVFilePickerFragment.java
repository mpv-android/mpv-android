package is.xyz.mpv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import is.xyz.filepicker.FilePickerFragment;

import java.io.File;

public class MPVFilePickerFragment extends FilePickerFragment {

    private File rootPath = new File("/");
    public boolean naturalSort = false;

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
    protected int compareFiles(@NonNull File lhs, @NonNull File rhs) {
        final boolean ldir = lhs.isDirectory(), rdir = rhs.isDirectory();
        if (ldir != rdir)
            return rdir ? 1 : -1;
        if (naturalSort)
            return naturalCompare(lhs.getName(), rhs.getName());
        return lhs.getName().compareToIgnoreCase(rhs.getName());
    }

    MPVFilePickerFragment() {
        USE_ALL_FILE_ACCESS = BuildConfig.FLAVOR.equals("allstorage");
    }

    @Override
    public void onClickCheckable(@NonNull View v, @NonNull FileViewHolder vh) {
        mListener.onFilePicked(vh.file);
    }

    @Override
    public boolean onLongClickCheckable(@NonNull View v, @NonNull DirViewHolder vh) {
        mListener.onDirPicked(vh.file);
        return true;
    }

    @NonNull
    @Override
    public File getRoot() {
        return rootPath;
    }

    public void setRoot(@NonNull File path) {
        rootPath = path;
    }

    public boolean isBackTop() {
        return mCurrentPath.equals(getRoot());
    }

    private @NonNull String makeRelative(@NonNull String path) {
        String head = getRoot().toString();
        if (path.equals(head))
            return "";
        if (!head.endsWith("/"))
            head += "/";
        return path.startsWith(head) ? path.substring(head.length()) : path;
    }

    @Override
    public void onChangePath(File file) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (file != null && bar != null)
            bar.setSubtitle(makeRelative(file.getPath()));
    }
}
