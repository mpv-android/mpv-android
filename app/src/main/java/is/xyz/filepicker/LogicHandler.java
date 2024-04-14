/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package is.xyz.filepicker;

import androidx.annotation.NonNull;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.List;

/**
 * An interface for the methods required to handle backend-specific stuff.
 */
public interface LogicHandler<T> {

    int VIEWTYPE_HEADER = 0;
    int VIEWTYPE_DIR = 1;
    int VIEWTYPE_FILE = 2;

    /**
     * Return true if the path is a directory and not a file.
     *
     * @param path
     */
    boolean isDir(@NonNull final T path);

    /**
     * @param path
     * @return filename of path
     */
    @NonNull
    String getName(@NonNull final T path);

    /**
     * Return the path to the parent directory. Should return the root if
     * from is root.
     *
     * @param from path to a directory
     */
    @NonNull
    T getParent(@NonNull final T from);

    /**
     * Convert path to a string representation.
     */
    @NonNull
    String pathToString(@NonNull final T path);

    /**
     * Convert string representation back to a path.
     */
    @NonNull
    T pathFromString(@NonNull final String path);

    /**
     * Get the root path (lowest allowed).
     */
    @NonNull
    T getRoot();

    /**
     * Get a loader that lists the files in the current path,
     * and monitors changes.
     */
    @NonNull
    Loader<List<T>> getLoader();

    /**
     * Bind the header ".." which goes to parent folder.
     *
     * @param viewHolder
     */
    void onBindHeaderViewHolder(@NonNull AbstractFilePickerFragment<T>.HeaderViewHolder viewHolder);

    /**
     * Header is subtracted from the position
     *
     * @param parent
     * @param viewType
     * @return a view holder for a file or directory
     */
    @NonNull
    RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    /**
     * @param viewHolder to bind data from either a file or directory
     * @param position   0 - n, where the header has been subtracted
     * @param data
     */
    void onBindViewHolder(@NonNull AbstractFilePickerFragment<T>.DirViewHolder viewHolder,
                          int position, @NonNull T data);

    /**
     * @param position 0 - n, where the header has been subtracted
     * @param data
     * @return an integer greater than 0
     */
    int getItemViewType(int position, @NonNull T data);
}
