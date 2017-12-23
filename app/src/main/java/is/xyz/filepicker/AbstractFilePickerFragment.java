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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

/**
 * A fragment representing a list of Files.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link
 * OnFilePickedListener}
 * interface.
 */
public abstract class AbstractFilePickerFragment<T> extends Fragment
        implements LoaderManager.LoaderCallbacks<SortedList<T>>,
        LogicHandler<T> {

    // The different preset modes of operation. This impacts the behaviour
    // and possible actions in the UI.
    public static final int MODE_FILE = 0;
    public static final int MODE_DIR = 1;
    public static final int MODE_FILE_AND_DIR = 2;
    public static final int MODE_NEW_FILE = 3;
    // Where to display on open.
    public static final String KEY_START_PATH = "KEY_START_PATH";
    // See MODE_XXX constants above for possible values
    public static final String KEY_MODE = "KEY_MODE";
    // If it should be possible to create directories.
    public static final String KEY_ALLOW_DIR_CREATE = "KEY_ALLOW_DIR_CREATE";
    // Allow multiple items to be selected.
    public static final String KEY_ALLOW_MULTIPLE = "KEY_ALLOW_MULTIPLE";
    // Allow an existing file to be selected under MODE_NEW_FILE
    public static final String KEY_ALLOW_EXISTING_FILE = "KEY_ALLOW_EXISTING_FILE";
    // If file can be selected by clicking only and checkboxes are not visible
    public static final String KEY_SINGLE_CLICK = "KEY_SINGLE_CLICK";
    // Used for saving state.
    protected static final String KEY_CURRENT_PATH = "KEY_CURRENT_PATH";
    protected int mode = MODE_FILE;
    protected T mCurrentPath = null;
    protected boolean allowCreateDir = false;
    protected boolean allowMultiple = false;
    protected boolean allowExistingFile = true;
    protected boolean singleClick = false;
    protected OnFilePickedListener mListener;
    protected FileItemAdapter<T> mAdapter = null;
    protected TextView mCurrentDirView;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;
    protected SortedList<T> mFiles = null;
    // Keep track if we are currently loading a directory, in case it takes a long time
    protected boolean isLoading = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AbstractFilePickerFragment() {
        // Retain this fragment across configuration changes, to allow
        // asynctasks and such to be used with ease.
        setRetainInstance(true);
    }

    protected FileItemAdapter<T> getDummyAdapter() {
        return new FileItemAdapter<>(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflateRootView(inflater, container);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.nnf_picker_toolbar);
        if (toolbar != null) {
            setupToolbar(toolbar);
        }

        recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        // improve performance if you know that changes in content
        // do not change the size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        // Set Item Decoration if exists
        configureItemDecoration(inflater, recyclerView);
        // Set adapter
        mAdapter = new FileItemAdapter<>(this);
        recyclerView.setAdapter(mAdapter);

        mCurrentDirView = (TextView) view.findViewById(R.id.nnf_current_dir);
        // Restore state
        if (mCurrentPath != null && mCurrentDirView != null) {
            mCurrentDirView.setText(getFullPath(mCurrentPath));
        }

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

    /**
     * Configure the toolbar anyway you like here. Default is to set it as the activity's
     * main action bar. Override if you already provide an action bar.
     * Not called if no toolbar was found.
     *
     * @param toolbar from layout with id "picker_toolbar"
     */
    protected void setupToolbar(@NonNull Toolbar toolbar) {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    public boolean isCheckable(@NonNull final T data) {
        final boolean checkable;
        if (isDir(data)) {
            checkable = ((mode == MODE_DIR && allowMultiple) ||
                    (mode == MODE_FILE_AND_DIR && allowMultiple));
        } else {
            // File
            checkable = (mode == MODE_FILE || mode == MODE_FILE_AND_DIR || allowExistingFile);
        }
        return checkable;
    }

    @Override
    public void onAttach(Context context) {
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
        // Only if we have no state
        if (mCurrentPath == null) {
            if (savedInstanceState != null) {
                mode = savedInstanceState.getInt(KEY_MODE, mode);
                allowCreateDir = savedInstanceState
                        .getBoolean(KEY_ALLOW_DIR_CREATE, allowCreateDir);
                allowMultiple = savedInstanceState
                        .getBoolean(KEY_ALLOW_MULTIPLE, allowMultiple);
                allowExistingFile = savedInstanceState
                        .getBoolean(KEY_ALLOW_EXISTING_FILE, allowExistingFile);
                singleClick = savedInstanceState
                        .getBoolean(KEY_SINGLE_CLICK, singleClick);

                String path = savedInstanceState.getString(KEY_CURRENT_PATH);
                if (path != null) {
                    mCurrentPath = getPath(path.trim());
                }
            } else if (getArguments() != null) {
                mode = getArguments().getInt(KEY_MODE, mode);
                allowCreateDir = getArguments()
                        .getBoolean(KEY_ALLOW_DIR_CREATE, allowCreateDir);
                allowMultiple = getArguments()
                        .getBoolean(KEY_ALLOW_MULTIPLE, allowMultiple);
                allowExistingFile = getArguments()
                        .getBoolean(KEY_ALLOW_EXISTING_FILE, allowExistingFile);
                singleClick = getArguments()
                        .getBoolean(KEY_SINGLE_CLICK, singleClick);
                if (getArguments().containsKey(KEY_START_PATH)) {
                    String path = getArguments().getString(KEY_START_PATH);
                    if (path != null) {
                        T file = getPath(path.trim());
                        if (isDir(file)) {
                            mCurrentPath = file;
                        } else {
                            mCurrentPath = getParent(file);
                        }
                    }
                }
            }
        }

        setModeView();

        // If still null
        if (mCurrentPath == null) {
            mCurrentPath = getRoot();
        }
        refresh(mCurrentPath);
    }

    /**
     * Hides/Shows appropriate views depending on mode
     */
    protected void setModeView() {
        boolean nf = mode == MODE_NEW_FILE;

        if (!nf && singleClick) {
            getActivity().findViewById(R.id.nnf_button_ok).setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.picker_actions, menu);

        MenuItem item = menu.findItem(R.id.nnf_action_createdir);
        item.setVisible(allowCreateDir);
    }


    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString(KEY_CURRENT_PATH, mCurrentPath.toString());
        b.putBoolean(KEY_ALLOW_MULTIPLE, allowMultiple);
        b.putBoolean(KEY_ALLOW_EXISTING_FILE, allowExistingFile);
        b.putBoolean(KEY_ALLOW_DIR_CREATE, allowCreateDir);
        b.putBoolean(KEY_SINGLE_CLICK, singleClick);
        b.putInt(KEY_MODE, mode);
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
    protected void refresh(@NonNull T nextPath) {
        if (hasPermission(nextPath)) {
            mCurrentPath = nextPath;
            isLoading = true;
            getLoaderManager()
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
    @Override
    public Loader<SortedList<T>> onCreateLoader(final int id, final Bundle args) {
        return getLoader();
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(final Loader<SortedList<T>> loader,
                               final SortedList<T> data) {
        isLoading = false;
        mFiles = data;
        mAdapter.setList(data);
        if (mCurrentDirView != null) {
            mCurrentDirView.setText(getFullPath(mCurrentPath));
        }
        // Stop loading now to avoid a refresh clearing the user's selections
        getLoaderManager().destroyLoader( 0 );
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(final Loader<SortedList<T>> loader) {
        isLoading = false;
    }

    /**
     * @param position 0 - n, where the header has been subtracted
     * @param data     the actual file or directory
     * @return an integer greater than 0
     */
    @Override
    public int getItemViewType(int position, @NonNull T data) {
        if (isCheckable(data)) {
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
                return new CheckableViewHolder(v);
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
     * Browses to the parent directory from the current directory. For example, if the current
     * directory is /foo/bar/, then goUp() will change the current directory to /foo/. It is up to
     * the caller to not call this in vain, e.g. if you are already at the root.
     * <p/>
     * Currently selected items are cleared by this operation.
     */
    public void goUp() {
        goToDir(getParent(mCurrentPath));
    }

    /**
     * Called when a non-selectable item, typically a directory, is clicked.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     */
    public void onClickDir(@NonNull View view, @NonNull DirViewHolder viewHolder) {
        if (isDir(viewHolder.file)) {
            goToDir(viewHolder.file);
        }
    }

    /**
     * Cab be used by the list to determine whether a file should be displayed or not.
     * Default behavior is to always display folders. If files can be selected,
     * then files are also displayed. In case a new file is supposed to be selected,
     * the {@link #allowExistingFile} determines if existing files are visible
     *
     * @param file either a directory or file.
     * @return True if item should be visible in the picker, false otherwise
     */
    protected boolean isItemVisible(final T file) {
        return (isDir(file) ||
                (mode == MODE_FILE || mode == MODE_FILE_AND_DIR) ||
                (mode == MODE_NEW_FILE && allowExistingFile));
    }

    /**
     * Browses to the designated directory. It is up to the caller verify that the argument is
     * in fact a directory. If another directory is in the process of being loaded, this method
     * will not start another load.
     * <p/>
     * Currently selected items are cleared by this operation.
     *
     * @param file representing the target directory.
     */
    public void goToDir(@NonNull T file) {
        if (!isLoading) {
            refresh(file);
        }
    }

    /**
     * Long clicking a non-selectable item does nothing by default.
     *
     * @param view       which was long clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     * @return true if the callback consumed the long click, false otherwise.
     */
    public boolean onLongClickDir(@NonNull View view, @NonNull DirViewHolder viewHolder) {
        return false;
    }

    /**
     * Called when a selectable item is clicked. This might be either a file or a directory.
     *
     * @param view       that was clicked. Not used in default implementation.
     * @param viewHolder for the clicked view
     */
    public void onClickCheckable(@NonNull View view, @NonNull CheckableViewHolder viewHolder) {
        if (isDir(viewHolder.file)) {
            goToDir(viewHolder.file);
        } else {
            if (singleClick) {
//                mListener.onFilePicked(toUri(getFirstCheckedItem()));
            }
        }
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
    public interface OnFilePickedListener {
        void onFilePicked(@NonNull File file);

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

    public class DirViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        public View icon;
        public TextView text;
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
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        @Override
        public boolean onLongClick(View v) {
            return onLongClickDir(v, this);
        }
    }

    public class CheckableViewHolder extends DirViewHolder {

        public CheckableViewHolder(View v) {
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
    }

}
