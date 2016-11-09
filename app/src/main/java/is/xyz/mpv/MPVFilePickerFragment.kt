package `is`.xyz.mpv

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.nononsenseapps.filepicker.AbstractFilePickerFragment

import com.nononsenseapps.filepicker.FilePickerFragment
import com.nononsenseapps.filepicker.LogicHandler

import java.io.File

class MPVFilePickerFragment : FilePickerFragment() {

    override fun setupToolbar(toolbar: Toolbar) {
        toolbar.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }

    override fun onClickCheckable(v: View, vh: AbstractFilePickerFragment<File>.CheckableViewHolder) {
        if (!allowMultiple) {
            // Clear is necessary, in case user clicked some checkbox directly
            mCheckedItems.clear()
            mCheckedItems.add(vh.file)
            onClickOk(null)
        } else {
            super.onClickCheckable(v, vh)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType != LogicHandler.VIEWTYPE_CHECKABLE)
            return super.onCreateViewHolder(parent, viewType)
        val v = LayoutInflater.from(activity).inflate(R.layout.filepicker_item, parent, false)
        return CheckableViewHolder(v)
    }

    val isBackTop: Boolean
        get() = compareFiles(mCurrentPath, File("/")) == 0
}
