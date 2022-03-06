package `is`.xyz.mpv

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.app.UiModeManager
import android.content.res.Configuration
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

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
        val recycler: RecyclerView = findViewById(android.R.id.list)
        recycler.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom)
            insets
        }

        supportActionBar?.setTitle(R.string.mpv_activity)

        if (PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            initFilePicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // restart activity to init file picker correctly
            finish()
            startActivity(intent)
        }
    }

    private fun initFilePicker() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (sharedPrefs.getBoolean("${localClassName}_filter_state", false)) {
            fragment!!.filterPredicate = MEDIA_FILE_FILTER
        }

        // TODO: rework or remove this setting
        val defaultPathStr = sharedPrefs.getString("default_file_manager_path",
                Environment.getExternalStorageDirectory().path)
        val defaultPath = File(defaultPathStr)

        // check that the preferred path is inside a storage volume
        val vols = Utils.getStorageVolumes(this)
        val vol = vols.find { defaultPath.startsWith(it.path) }
        if (vol == null) {
            // looks like it wasn't
            Log.w(TAG, "default path set to $defaultPath but no such storage volume")
            with (fragment!!) {
                root = vols.first().path
                goToDir(vols.first().path)
            }
        } else {
            with (fragment!!) {
                root = vol.path
                goToDir(defaultPath)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION)
            menuInflater.inflate(R.menu.menu_main, menu)
        else
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "...") // dummy menu item to indicate presence
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_external_storage) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                val path = Environment.getExternalStorageDirectory()
                fragment!!.goToDir(path) // do something potentially useful
                return true
            }

            val vols = Utils.getStorageVolumes(this)

            with (AlertDialog.Builder(this)) {
                setItems(vols.map { it.description }.toTypedArray()) { dialog, item ->
                    val vol = vols[item]
                    with (fragment!!) {
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
            with (fragment!!) {
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

    override fun dispatchKeyEvent(ev: KeyEvent?): Boolean {
        var openMenu = false
        if (ev?.action == KeyEvent.ACTION_DOWN && ev.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // If up is pressed at the header element display the usual options menu as a popup menu
            // to make it usable on Android TV.
            val recycler: RecyclerView = findViewById(android.R.id.list)
            val holder = try {
                window.currentFocus?.let { recycler.getChildViewHolder(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
            openMenu = (holder is AbstractFilePickerFragment<*>.HeaderViewHolder)
        }
        if (openMenu) {
            PopupMenu(this, findViewById(R.id.context_anchor)).apply {
                setOnMenuItemClickListener {
                    this@MainActivity.onOptionsItemSelected(it)
                }
                inflate(R.menu.menu_main)
                show()
            }
            return true
        }

        return super.dispatchKeyEvent(ev)
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
                Utils.MEDIA_EXTENSIONS.contains(file.extension.toLowerCase())
            }
        }
    }
}
