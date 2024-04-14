/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package is.xyz.filepicker;

import is.xyz.mpv.R;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.FileObserver;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An implementation of the picker which allows you to select a file from the internal/external
 * storage (SD-card) on a device.
 */
public class FilePickerFragment extends AbstractFilePickerFragment<File> {

    protected static final int PERMISSIONS_REQUEST_ID = 1001;
    protected static final String PERMISSION_PRE33 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    protected static final String[] PERMISSIONS_POST33 = {
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
    };
    protected boolean showHiddenItems = false;
    protected FileFilter filterPredicate = null;
    private File mRequestedPath = null;

    public FilePickerFragment() {
    }

    /**
     * This method is used to dictate whether hidden files and folders should be shown or not
     *
     * @param showHiddenItems whether hidden items should be shown or not
     */
    public void showHiddenItems(boolean showHiddenItems){
        this.showHiddenItems = showHiddenItems;
    }

    /**
     * Returns if hidden items are shown or not
     *
     * @return true if hidden items are shown, otherwise false
     */

    public boolean areHiddenItemsShown(){
        return showHiddenItems;
    }

    /**
     * This method is used to set the filter that determines the files to be shown
     *
     * @param predicate filter implementation or null
     */
    public void setFilterPredicate(@Nullable FileFilter predicate) {
        this.filterPredicate = predicate;
        refresh(mCurrentPath);
    }

    /**
     * Returns the filter that determines the files to be shown
     *
     * @return filter implementation or null
     */
    public @Nullable FileFilter getFilterPredicate() {
        return this.filterPredicate;
    }

    /**
     * @return true if app has been granted permission to write to the SD-card.
     */
    public static boolean hasPermission(@NonNull Context context, @NonNull File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // user can choose to grant at least one
            for (String permission : PERMISSIONS_POST33) {
                if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(context, permission)) {
                    return true;
                }
            }
            return false;
        } else {
            return PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(context, PERMISSION_PRE33);
        }
    }

    @Override
    protected boolean hasPermission(@NonNull File path) {
        return hasPermission(requireContext(), path);
    }

    /**
     * Request permission to write to the SD-card.
     */
    @Override
    protected void handlePermission(@NonNull File path) {
        mRequestedPath = path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(PERMISSIONS_POST33, PERMISSIONS_REQUEST_ID);
        } else {
            requestPermissions(new String[]{PERMISSION_PRE33}, PERMISSIONS_REQUEST_ID);
        }
    }

    /**
     * This the method that gets notified when permission is granted/denied. By default,
     * a granted request will result in a refresh of the list.
     *
     * @param requestCode  the code you requested
     * @param permissions  array of permissions you requested. empty if process was cancelled.
     * @param grantResults results for requests. empty if process was cancelled.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST_ID)
            return;
        // If arrays are empty, then process was cancelled
        if (permissions.length == 0) {
            // Treat this as a cancel press
            if (mListener != null)
                mListener.onCancelled();
            return;
        }
        boolean ok = false;
        for (int r : grantResults) {
            if (PackageManager.PERMISSION_GRANTED == r) {
                ok = true;
                break;
            }
        }
        if (ok) {
            // Do refresh
            if (mRequestedPath != null)
                refresh(mRequestedPath);
        } else {
            Toast.makeText(getContext(), R.string.nnf_permission_external_write_denied,
                    Toast.LENGTH_SHORT).show();
            // Treat this as a cancel press
            if (mListener != null)
                mListener.onCancelled();
        }
    }

    /**
     * Return true if the path is a directory and not a file.
     *
     * @param path either a file or directory
     * @return true if path is a directory, false if file
     */
    @Override
    public boolean isDir(@NonNull final File path) {
        return path.isDirectory();
    }

    /**
     * @param path either a file or directory
     * @return filename of path
     */
    @NonNull
    @Override
    public String getName(@NonNull File path) {
        return path.getName();
    }

    /**
     * Return the path to the parent directory. Should return the root if
     * from is root.
     *
     * @param from either a file or directory
     * @return the parent directory
     */
    @NonNull
    @Override
    public File getParent(@NonNull final File from) {
        if (from.getPath().equals(getRoot().getPath())) {
            // Already at root, we can't go higher
            return from;
        } else if (from.getParentFile() != null) {
            return from.getParentFile();
        } else {
            return from;
        }
    }

    /**
     * Convert the path to the type used.
     *
     * @param path either a file or directory
     * @return File representation of the string path
     */
    @NonNull
    @Override
    public File pathFromString(@NonNull final String path) {
        return new File(path);
    }

    /**
     * @param path either a file or directory
     * @return the full path to the file
     */
    @NonNull
    @Override
    public String pathToString(@NonNull final File path) {
        return path.getPath();
    }

    /**
     * Get the root path.
     *
     * @return the highest allowed path, which is "/" by default
     */
    @NonNull
    @Override
    public File getRoot() {
        return new File("/");
    }

    /**
     * Get a loader that lists the Files in the current path,
     * and monitors changes.
     */
    @NonNull
    @Override
    public Loader<List<File>> getLoader() {
        return new AsyncTaskLoader<>(requireContext()) {
            FileObserver fileObserver;

            @Override
            public List<File> loadInBackground() {
                File[] listFiles = mCurrentPath.listFiles();
                if (listFiles == null) {
                    Log.e(TAG, "FilePickerFragment: IO error while listing files");
                    return new ArrayList<>(0);
                }

                ArrayList<File> files = new ArrayList<>(listFiles.length);
                for (File f : listFiles) {
                    if (f.isHidden() && !areHiddenItemsShown())
                        continue;
                    if (filterPredicate != null && !filterPredicate.accept(f))
                        continue;
                    files.add(f);
                }

                Collections.sort(files, (lhs, rhs) -> compareFiles(lhs, rhs));

                return files;
            }

            /**
             * Handles a request to start the Loader.
             */
            @Override
            protected void onStartLoading() {
                super.onStartLoading();

                // handle if directory does not exist. Fall back to root.
                if (mCurrentPath == null || !mCurrentPath.isDirectory()) {
                    mCurrentPath = getRoot();
                }

                // Start watching for changes
                fileObserver = new FileObserver(mCurrentPath.getPath(),
                        FileObserver.CREATE |
                                FileObserver.DELETE
                                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
                ) {

                    @Override
                    public void onEvent(int event, String path) {
                        // Reload
                        onContentChanged();
                    }
                };
                fileObserver.startWatching();

                forceLoad();
            }

            /**
             * Handles a request to completely reset the Loader.
             */
            @Override
            protected void onReset() {
                super.onReset();

                // Stop watching
                if (fileObserver != null) {
                    fileObserver.stopWatching();
                    fileObserver = null;
                }
            }
        };
    }

    /**
     * Compare two files to determine their relative sort order. This follows the usual
     * comparison interface. Override to determine your own custom sort order.
     * <p/>
     * Default behaviour is to place directories before files, but sort them alphabetically
     * otherwise.
     *
     * @param lhs File on the "left-hand side"
     * @param rhs File on the "right-hand side"
     * @return -1 if if lhs should be placed before rhs, 0 if they are equal,
     * and 1 if rhs should be placed before lhs
     */
    protected int compareFiles(@NonNull File lhs, @NonNull File rhs) {
        final boolean ldir = lhs.isDirectory(), rdir = rhs.isDirectory();
        if (ldir != rdir)
            return rdir ? 1 : -1;
        return lhs.getName().compareToIgnoreCase(rhs.getName());
    }

    private static final String TAG = "mpv";
}
