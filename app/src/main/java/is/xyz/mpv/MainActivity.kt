package `is`.xyz.mpv

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

import `is`.xyz.filepicker.AbstractFilePickerFragment
import `is`.xyz.mpv.config.SettingsActivity

import java.io.File
import java.io.FileFilter

class MainActivity : AppCompatActivity(), AbstractFilePickerFragment.OnFilePickedListener {

    private var fragment: MPVFilePickerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the system UI to act as if the nav bar is hidden, so that we can
        // draw behind it. STABLE flag is historically recommended but was
        // deprecated in API level 30, so probably not strictly necessary, but
        // cargo-culting is fun.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                              View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragment = supportFragmentManager.findFragmentById(R.id.file_picker_fragment) as MPVFilePickerFragment

        // With the app acting as if the navbar is hidden, we need to
        // account for it outselves. We want the recycler to directly
        // take the system UI padding so that we can tell it to draw
        // into the padded area while still respecting the padding for
        // input.
        val layout: RelativeLayout = findViewById(R.id.main_layout)
        val recycler: RecyclerView = layout.findViewById(android.R.id.list)
        recycler.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom)
            insets
        }

        supportActionBar?.setTitle(R.string.mpv_activity)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (sharedPrefs.getBoolean("${localClassName}_filter_state", false)) {
            (fragment as MPVFilePickerFragment).filterPredicate = MEDIA_FILE_FILTER
        }

        // TODO: rework or remove this setting
        val defaultPathStr = sharedPrefs.getString("default_file_manager_path",
                Environment.getExternalStorageDirectory().path)
        val defaultPath = File(defaultPathStr)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // check that the preferred path is inside a storage volume
            val vols = Utils.getStorageVolumes(this)
            val vol = vols.find { defaultPath.startsWith(it.path) }
            if (vol == null) {
                // looks like it wasn't
                Log.w(TAG, "default path set to $defaultPath but no such storage volume")
                with (fragment as MPVFilePickerFragment) {
                    root = vols.first().path
                    goToDir(vols.first().path)
                }
            } else {
                with (fragment as MPVFilePickerFragment) {
                    root = vol.path
                    goToDir(defaultPath)
                }
            }
        } else {
            // Old device: go to preferred path but don't restrict root
            (fragment as MPVFilePickerFragment).goToDir(defaultPath)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_external_storage) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                val path = Environment.getExternalStorageDirectory()
                (fragment as MPVFilePickerFragment).goToDir(path) // do something
                return true
            }

            val vols = Utils.getStorageVolumes(this)

            with (AlertDialog.Builder(this)) {
                setItems(vols.map { it.description }.toTypedArray()) { dialog, item ->
                    val vol = vols[item]
                    with (fragment as MPVFilePickerFragment) {
                        root = vol.path
                        goToDir(vol.path)
                    }
                    dialog.dismiss()
                }
                show()
            }
            return true
        } else if (id == R.id.action_file_filter) {
            val old: Boolean
            with (fragment as MPVFilePickerFragment) {
                old = filterPredicate != null
                filterPredicate = if (!old) MEDIA_FILE_FILTER else null
            }
            with (Toast.makeText(this, "", Toast.LENGTH_SHORT)) {
                setText(if (!old) R.string.notice_show_media_files else R.string.notice_show_all_files)
                show()
            }
            // remember state for next time
            with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
                this.putBoolean("${localClassName}_filter_state", !old)
                apply()
            }
            return true
        } else if (id == R.id.action_open_url) {
            // https://stackoverflow.com/questions/10903754/#answer-10904665
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

            with (AlertDialog.Builder(this)) {
                setTitle(R.string.action_open_url)
                setView(input)
                setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                    playFile(input.text.toString())
                }
                setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                    dialog.cancel()
                }
                show()
            }
        } else if (id == R.id.action_settings) {
            val i = Intent(this, SettingsActivity::class.java)
            startActivity(i)
            return true
        }
        return false
    }

    private fun playFile(filepath: String) {
        val i = Intent(this, MPVActivity::class.java)
        i.putExtra("filepath", filepath)
        startActivity(i)
    }

    override fun onFilePicked(file: File) {
        playFile(file.absolutePath)
    }

    override fun onDirPicked(dir: File) {
        // mpv will play directories as playlist of all contained files
        playFile(dir.absolutePath)
    }

    override fun onCancelled() {
    }

    override fun onBackPressed() {
        if (fragment!!.isBackTop) {
            super.onBackPressed()
        } else {
            fragment!!.goUp()
        }
    }

    companion object {
        private const val TAG = "mpv"

        private val MEDIA_FILE_FILTER = FileFilter { file ->
            if (file.isDirectory) {
                val contents: Array<String> = file.list() ?: arrayOf()
                // filter hidden files due to stuff like ".thumbnails"
                contents.filterNot { it.startsWith('.') }.any()
            } else {
                Utils.MEDIA_EXTENSIONS.contains(file.extension)
            }
        }
    }
}
