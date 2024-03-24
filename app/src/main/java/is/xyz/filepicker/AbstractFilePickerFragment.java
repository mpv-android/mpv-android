/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package is.xyz.filepicker;

import is.xyz.mpv.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Files.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link
 * OnFilePickedListener}
 * interface.
 */
public abstract class AbstractFilePickerFragment<T> extends Fragment
        implements LoaderManager.LoaderCallbacks<List<T>>,
        LogicHandler<T> {

    protected T mCurrentPath = null;
    protected OnFilePickedListener mListener;
    protected FileItemAdapter<T> mAdapter = null;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;
    protected List<T> mFiles = null;
    // Keep track if we are currently loading a directory, in case it takes a long time
    protected boolean isLoading = false;

    private HashMap<String, Integer> mPositionMap;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AbstractFilePickerFragment() {
        // Retain this fragment across configuration changes, to allow
        // asynctasks and such to be used with ease.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflateRootView(inflater, container);

        mPositionMap = new HashMap<>();

        recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        // improve performance if you know that changes in content
        // do not change the size of the RecyclerView
        //noinspection InvalidSetHasFixedSize
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        // Set Item Decoration if exists
        configureItemDecoration(inflater, recyclerView);
        // Set adapter
        mAdapter = new FileItemAdapter<>(this);
        recyclerView.setAdapter(mAdapter);

        onChangePath(mCurrentPath);

        return view;
    }

    protected View inflateRootView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate( R.layout.nnf_fragment_filepicker, container, false);
    }

    /**
     * Checks if a divider drawable has been defined in the current theme. If it has, will apply
     * an item decoration with the divider. If no divider has been specified, then does nothing.
     */
    protected void configureItemDecoration(@NonNull LayoutInflater inflater,
                                           @NonNull RecyclerView recyclerView) {
        final TypedArray attributes =
                getActivity().obtainStyledAttributes(new int[]{R.attr.nnf_list_item_divider});
        Drawable divider = attributes.getDrawable(0);
        attributes.recycle();

        if (divider != null) {
            recyclerView.addItemDecoration(new DividerItemDecoration(divider));
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFilePickedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnFilePickedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Fall back to root if still null
        if (mCurrentPath == null)
            mCurrentPath = getRoot();
        refresh(mCurrentPath);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Refreshes the list. Call this when current path changes. This method also checks
     * if permissions are granted and requests them if necessary. See hasPermission()
     * and handlePermission(). By default, these methods do nothing. Override them if
     * you need to request permissions at runtime.
     *
     * @param nextPath path to list files for
     */
    protected void refresh(T nextPath) {
        if (nextPath == null)
            return;
        mCurrentPath = nextPath;
        // Skip loading anything if not initialized yet
        if (getContext() == null)
            return;
        isLoading = true;
        if (hasPermission(nextPath)) {
            LoaderManager.getInstance(this)
                    .restartLoader(0, null, AbstractFilePickerFragment.this);
        } else {
            handlePermission(nextPath);
        }
    }

    /**
     * If permission has not been granted yet, this method should request it.
     * <p/>
     * Override only if you need to request a permission.
     *
     * @param path The path for which permission should be requested
     */
    protected void handlePermission(@NonNull T path) {
        // Nothing to do by default
    }

    /**
     * If your implementation needs to request a specific permission to function, check if it
     * has been granted here. You should probably also override handlePermission() to request it.
     *
     * @param path the path for which permissions should be checked
     * @return true if permission has been granted, false otherwise.
     */
    protected boolean hasPermission(@NonNull T path) {
        // Nothing to request by default
        return true;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @NonNull
    @Override
    public Loader<List<T>> onCreateLoader(final int id, final Bundle args) {
        return getLoader();
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull final Loader<List<T>> loader,
                               final List<T> data) {
        isLoading = false;
        mFiles = data;
        mAdapter.setList(data);
        onChangePath(mCurrentPath);
        String key = pathToString(mCurrentPath);
        if (mPositionMap.containsKey(key))
            layoutManager.scrollToPositionWithOffset(mPositionMap.get(key), 0);
        else
            layoutManager.scrollToPositionWithOffset(0, 0);
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull final Loader<List<T>> loader) {
        isLoading = false;
    }

    /**
     * @param position 0 - n, where the header has been subtracted
     * @param data     the actual file or directory
     * @return an integer greater than 0
     */
    @Override
    public int getItemViewType(int position, @NonNull T data) {
        if (!isDir(data)) {
            return LogicHandler.VIEWTYPE_FILE;
        } else {
            return LogicHandler.VIEWTYPE_DIR;
        }
    }

    @Override
    public void onBindHeaderViewHolder(@NonNull HeaderViewHolder viewHolder) {
        viewHolder.text.setText("..");
    }

    /**
     * @param parent   Containing view
     * @param viewType which the ViewHolder will contain
     * @return a view holder for a file or directory
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case LogicHandler.VIEWTYPE_HEADER:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.nnf_filepicker_listitem_dir,
                        parent, false);
                return new HeaderViewHolder(v);
            case LogicHandler.VIEWTYPE_FILE:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.nnf_filepicker_listitem_checkable,
                        parent, false);
                return new FileViewHolder(v);
            case LogicHandler.VIEWTYPE_DIR:
            default:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.nnf_filepicker_listitem_dir,
                        parent, false);
                return new DirViewHolder(v);
        }
    }

    /**
     * @param vh       to bind data from either a file or directory
     * @param position 0 - n, where the header has been subtracted
     * @param data     the file or directory which this item represents
     */
    @Override
    public void onBindViewHolder(@NonNull DirViewHolder vh, int position, @NonNull T data) {
        vh.file = data;
        vh.icon.setVisibility(isDir(data) ? View.VISIBLE : View.GONE);
        vh.text.setText(getName(data));
    }


    /**
     * Called when a header item ("..") is clicked.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     */
    public void onClickHeader(@NonNull View view, @NonNull HeaderViewHolder viewHolder) {
        goUp();
    }

    /**
     * Returns the directory currently being viewed.
     */
    public T getCurrentDir() {
        return mCurrentPath;
    }

    /**
     * Browses to the parent directory from the current directory. For example, if the current
     * directory is /foo/bar/, then goUp() will change the current directory to /foo/. It is up to
     * the caller to not call this in vain, e.g. if you are already at the root.
     * <p/>
     */
    public void goUp() {
        String key = pathToString(mCurrentPath);
        mPositionMap.remove(key);
        goToDir(getParent(mCurrentPath), false);
    }

    /**
     * Called when a non-selectable item, typically a directory, is clicked.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     */
    public void onClickDir(@NonNull View view, @NonNull DirViewHolder viewHolder) {
        if (isDir(viewHolder.file)) {
            String key = pathToString(mCurrentPath);
            mPositionMap.put(key, layoutManager.findFirstVisibleItemPosition());
            goToDir(viewHolder.file, false);
        }
    }

    /**
     * Browses to the designated directory. It is up to the caller verify that the argument is
     * in fact a directory.
     *
     * @param file representing the target directory.
     */
    public void goToDir(@NonNull T file) {
        goToDir(file, true);
    }

    protected void goToDir(@NonNull T file, boolean force) {
        if (!isLoading || force)
            refresh(file);
    }

    /**
     * Called when a selectable item is clicked. The item will be a file.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     */
    public void onClickCheckable(@NonNull View view, @NonNull FileViewHolder viewHolder) {

    }

    /**
     * Called when a selectable item is long clicked. The item will be a directory.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     * @return true if the callback consumed the long click, false otherwise.
     */
    public boolean onLongClickCheckable(@NonNull View view, @NonNull DirViewHolder viewHolder) {
        return false;
    }

    public void onChangePath(T file) {
        // No default implementation
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating
     * .html"
     * >Communicating with Other Fragments</a> for more information.
     */
    // FIXME: this interface is terrible
    public interface OnFilePickedListener {
        void onFilePicked(@NonNull File file);
        void onDirPicked(@NonNull File dir);
        void onDocumentPicked(@NonNull Uri uri, boolean isDir);

        void onCancelled();
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView text;

        public HeaderViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            text = (TextView) v.findViewById(android.R.id.text1);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            onClickHeader(v, this);
        }
    }

    public class DirViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public final View icon;
        public final TextView text;
        public T file;

        public DirViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            icon = v.findViewById(R.id.item_icon);
            text = (TextView) v.findViewById(android.R.id.text1);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            onClickDir(v, this);
        }

        /**
         * Called when a view has been long clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public boolean onLongClick(View v) { return onLongClickCheckable(v, this); }
    }

    public class FileViewHolder extends DirViewHolder {

        public FileViewHolder(View v) {
            super(v);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            onClickCheckable(v, this);
        }

        /**
         * Called when a view has been long clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public boolean onLongClick(View v) { return false; }
    }

}
