package is.xyz.filepicker;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * An implementation of the picker that operates on a document tree.
 *
 * See also:
 * - https://developer.android.com/training/data-storage/shared/documents-files
 * - https://developer.android.com/reference/android/provider/DocumentsContract.Document#MIME_TYPE_DIR
 * - https://github.com/android/storage-samples/blob/main/ActionOpenDocumentTree/app/src/main/java/com/example/android/ktfiles/DirectoryFragmentViewModel.kt#L42
 * - https://github.com/googlearchive/android-DirectorySelection/blob/master/Application/src/main/java/com/example/android/directoryselection/DirectorySelectionFragment.java
 */
public class DocumentPickerFragment extends AbstractFilePickerFragment<Uri> {
    private final @NonNull Uri mRoot;
    // The structure of the file picker assumes that only the file URIs matter and you can
    // grab additional info for free afterwards. This is not the case with the documents API so we
    // have to work around it.
    final HashMap<Uri, Document> mLastRead;

    public DocumentPickerFragment(@NonNull Uri root) {
        mRoot = root;
        mLastRead = new HashMap<>();
    }

    /**
     * Check whether the given tree can be used with this file picker class.
     * @param context Application context
     * @param treeUri Tree URI from e.g. ACTION_OPEN_DOCUMENT_TREE
     * @return true if the directory exists
     */
    public static boolean isTreeUsable(@NonNull Context context, @NonNull Uri treeUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!DocumentsContract.isTreeUri(treeUri))
                return false;
        }
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));
        return isDir(context, docUri);
    }

    @Override
    public boolean isDir(@NonNull Uri path) {
        Document doc = mLastRead.get(path);
        if (doc != null) {
            return doc.isDir;
        }

        // retrieve the data uncached (not supposed to happen)
        return isDir(requireContext(), path);
    }

    private static boolean isDir(@NonNull Context context, @NonNull Uri path) {
        final ContentResolver contentResolver = context.getContentResolver();
        final String[] cols = new String[] { DocumentsContract.Document.COLUMN_MIME_TYPE };
        Cursor c = contentResolver.query(path, cols, null, null, null, null);
        boolean ret = false;
        if (c == null)
            return ret;
        if (c.moveToFirst()) {
            final int i = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
            ret = c.getString(i).equals(DocumentsContract.Document.MIME_TYPE_DIR);
        }
        c.close();
        return ret;
    }

    @NonNull
    @Override
    public String getName(@NonNull Uri path) {
        Document doc = mLastRead.get(path);
        if (doc != null) {
            return doc.displayName;
        }

        // retrieve the data uncached (not supposed to happen)
        final ContentResolver contentResolver = requireContext().getContentResolver();
        final String[] cols = new String[] { DocumentsContract.Document.COLUMN_DISPLAY_NAME };
        Cursor c = contentResolver.query(path, cols, null, null, null, null);
        String ret = "";
        if (c == null)
            return ret;
        if (c.moveToFirst()) {
            final int i = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            ret = c.getString(i);
        }
        c.close();
        return ret;
    }

    @NonNull
    @Override
    public Uri toUri(@NonNull Uri path) {
        return path;
    }

    @NonNull
    @Override
    public Uri getParent(@NonNull Uri from) {
        Document doc = mLastRead.get(from);
        if (doc != null) {
            return doc.parent;
        }
        // This is not supposed to happen
        return getRoot();
    }

    @NonNull
    @Override
    public String getFullPath(@NonNull Uri path) {
        return path.toString();
    }

    @NonNull
    @Override
    public Uri getPath(@NonNull String path) {
        return Uri.parse(path);
    }

    @NonNull
    @Override
    public Uri getRoot() {
        return mRoot;
    }

    @NonNull
    @Override
    public Loader<List<Uri>> getLoader() {
        final Uri root = mRoot;
        final Uri currentPath = mCurrentPath;

        // totally makes sense!
        final String docId = mCurrentPath.equals(root) ? DocumentsContract.getTreeDocumentId(currentPath) :
                DocumentsContract.getDocumentId(currentPath);
        final Uri childUri = DocumentsContract.buildChildDocumentsUriUsingTree(root, docId);

        final String[] cols = new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        };
        return new AsyncTaskLoader<List<Uri>>(requireContext()) {
            @Override
            public List<Uri> loadInBackground() {
                final ContentResolver contentResolver = getContext().getContentResolver();
                Cursor c = contentResolver.query(childUri, cols, null, null, null, null);
                if (c == null) {
                    return new ArrayList<>(0);
                }

                ArrayList<Document> files = new ArrayList<>();
                final int i1 = c.getColumnIndex(cols[0]), i2 = c.getColumnIndex(cols[1]), i3 = c.getColumnIndex(cols[2]);
                while (c.moveToNext()) {
                    // TODO later: support FileFilter equivalent here
                    files.add(new Document(
                            DocumentsContract.buildDocumentUriUsingTree(root, c.getString(i1)),
                            currentPath,
                            c.getString(i2).equals(DocumentsContract.Document.MIME_TYPE_DIR),
                            c.getString(i3)
                    ));
                }
                c.close();

                Collections.sort(files);

                // extract the URIs because we (can) only return those
                ArrayList<Uri> ret = new ArrayList<>(files.size());
                for (Document doc : files)
                    ret.add(doc.uri);
                // but keep the cached data
                mLastRead.clear();
                for (Document doc : files)
                    mLastRead.put(doc.uri, doc);
                return ret;
            }

            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                forceLoad();
            }
        };
    }

    /**
     * Class that represents a document.
     * Wrapper around a content:// URI but with extra information provided at no extra cost (cached).
     */
    private static class Document implements Comparable<Document> {
        private final @NonNull Uri uri;
        private final @NonNull Uri parent;
        private final boolean isDir;
        private final @NonNull String displayName;

        private Document(@NonNull Uri uri, @NonNull Uri parent, boolean dir, @NonNull String name) {
            this.uri = uri;
            this.parent = parent;
            isDir = dir;
            displayName = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            // Cached info is irrelevant, same URI = same document
            return uri.equals(((Document) o).uri);
        }

        // Sort directories before files, alphabetically otherwise
        @Override
        public int compareTo(Document other) {
            if (isDir && !other.isDir) {
                return -1;
            } else if (other.isDir && !isDir) {
                return 1;
            } else {
                return displayName.compareToIgnoreCase(other.displayName);
            }
        }
    }
}
