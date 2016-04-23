package `is`.xyz.mpv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import com.nononsenseapps.filepicker.FilePickerActivity

import java.io.File

class MainActivity : FilePickerActivity() {
    private val CODE_FILE: Int = 0;
    private var currentFragment: MPVFilePickerFragment? = null

    override fun getFragment(startPath: String?, mode: Int, allowMultiple: Boolean, allowCreateDir: Boolean): AbstractFilePickerFragment<File>? {
        setFragement()
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"

        var path = startPath

        if (path == null) {
            path = Environment.getExternalStorageDirectory().path
        }

        (currentFragment as MPVFilePickerFragment).setArgs(path, mode, allowMultiple, allowCreateDir)

        return currentFragment
    }

    private fun setFragement() {
        if (currentFragment == null) {
            val thing = supportFragmentManager.findFragmentById(R.id.file_picker_fragment)
            if (thing == null) {
                currentFragment = MPVFilePickerFragment()
            } else {
                currentFragment = thing as MPVFilePickerFragment
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onFilePicked(file: Uri) {
        val f = File(file.path)
        playFile(f.absolutePath)
    }

    override fun onFilesPicked(files: List<Uri>) {
    }

    override fun onCancelled() {
    }

    override fun onBackPressed() {
        // setFragement()
        if (currentFragment == null || currentFragment!!.isBackTop) {
            super.onBackPressed()
        } else {
            currentFragment!!.goUp()
        }
    }

    override fun onResume() {
        super.onResume()

    }

    private fun playFile(filepath: String) {
        val i = Intent(this, MPVActivity::class.java)
        i.putExtra("filepath", filepath)
        startActivity(i)
    }
}
