package `is`.xyz.mpv.browse

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import `is`.xyz.filepicker.AbstractFilePickerFragment
import `is`.xyz.filepicker.FilePickerFragment
import `is`.xyz.mpv.FilePickerActivity
import `is`.xyz.mpv.MPVActivity
import `is`.xyz.mpv.MPVFilePickerFragment
import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.config.SettingsActivity
import `is`.xyz.mpv.databinding.ActivityBrowseBinding
import java.io.File
import java.io.FileFilter

class BrowseActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    AbstractFilePickerFragment.OnFilePickedListener {

    private lateinit var binding: ActivityBrowseBinding
    private lateinit var preferences: SharedPreferences
    private lateinit var fragment: MPVFilePickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        if (preferences.getBoolean("material_you_theming", false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode != Activity.RESULT_OK) {
                    return@registerForActivityResult
                }
                it.data?.getStringExtra("path")?.let { path ->
                    launchPlayer(path)
                }
            }

        val documentTreeOpener =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
                it?.let { root ->
                    contentResolver.takePersistableUriPermission(
                        root, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val i = Intent(this, FilePickerActivity::class.java)
                    i.putExtra("skip", FilePickerActivity.DOC_PICKER)
                    i.putExtra("root", root.toString())
                    filePickerLauncher.launch(i)
                }
            }

        with(binding) {

            openUrlButton.setOnClickListener {
                val helper = Utils.OpenUrlDialog(this@BrowseActivity)
                with(helper) {
                    builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                        launchPlayer(helper.text)
                    }
                    builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                    create().show()
                }
            }

            if (!hasDocumentTree()) openDocTreeButton.visibility = View.GONE
            openDocTreeButton.setOnClickListener {
                try {
                    documentTreeOpener.launch(null)
                } catch (e: ActivityNotFoundException) {
                    // Android TV doesn't come with a document picker and certain versions just throw
                    // instead of handling this gracefully
                }
            }


            if (!preferences.getBoolean(
                    "remember_last_playback", true
                ) || preferences.getString("lastPlayed", null).isNullOrBlank()
            ) {
                resumeLastPlayback.hide()
            }

            resumeLastPlayback.setOnClickListener {
                preferences.getString("lastPlayed", null)?.let { path ->
                    launchPlayer(path)
                }
            }

            initFilePicker()
        }
    }


    //These are SOO OLD bru, I'd refactor but not rn
    private fun initFilePicker() {
        if (!::fragment.isInitialized) {
            fragment = MPVFilePickerFragment()
            with(supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(binding.fragmentContainer.id, fragment, null)
                commit()
            }
        }

        if (!FilePickerFragment.hasPermission(this, File("/"))) return

        val defaultPathStr = preferences.getString(
            "default_file_manager_path",
            Environment.getExternalStorageDirectory().path
        )

        val defaultPath = File(defaultPathStr!!)

        // Old device: go to preferred path but don't restrict root
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return fragment.goToDir(defaultPath)

        // check that the preferred path is inside a storage volume
        val vols = Utils.getStorageVolumes(this)
        val vol = vols.find { defaultPath.startsWith(it.path) }

        with(fragment) {
            if (getFilterState()) filterPredicate = MEDIA_FILE_FILTER

            root = vol?.path ?: vols.first().path
            goToDir(if (vol == null) vols.first().path else defaultPath)
        }
    }

    private fun getFilterState(): Boolean {
        with(preferences) {
            return getBoolean("MainActivity_filter_state", false)
        }
    }

    private fun saveFilterState(enabled: Boolean) {
        with(preferences.edit()) {
            putBoolean("MainActivity_filter_state", enabled)
            apply()
        }
    }


    private fun hasDocumentTree(): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        return intent.resolveActivity(packageManager) != null
    }


    private fun launchPlayer(filepath: String) {
        preferences.edit().putString("lastPlayed", filepath).apply()
        binding.resumeLastPlayback.show()
        val i: Intent = when {
            filepath.startsWith("content://") -> Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
            else -> Intent().putExtra("filepath", filepath)
        }.setClass(this, MPVActivity::class.java)
        startActivity(i)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_browse, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_file_filter)?.icon?.let {
            if (getFilterState()) it.setTint(getThemeColor(this, android.R.attr.colorPrimary))
            else it.setTint(getThemeColor(this))
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }

            R.id.action_file_filter -> {
                var old: Boolean
                fragment.apply {
                    old = filterPredicate != null
                    filterPredicate = if (!old) MEDIA_FILE_FILTER else null
                }
                with(Toast.makeText(this, "", Toast.LENGTH_SHORT)) {
                    setText(if (!old) R.string.notice_show_media_files else R.string.notice_show_all_files)
                    show()
                }
                saveFilterState(!old)
                invalidateOptionsMenu()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onFilePicked(file: File) = launchPlayer(file.absolutePath)

    override fun onDirPicked(dir: File) = launchPlayer(dir.absolutePath)
    override fun onDocumentPicked(uri: Uri, isDir: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onCancelled() {
        TODO("Not yet implemented")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!::fragment.isInitialized) return
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // re-init file picker with correct paths
            initFilePicker()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "material_you_theming" -> {
                if (sharedPreferences.getBoolean(key, false)) {
                    DynamicColors.applyToActivityIfAvailable(this)
                }
                recreate()
            }


            "remember_last_playback", "lastPlayed" -> {
                binding.resumeLastPlayback.apply {
                    if (!sharedPreferences.getBoolean(
                            "remember_last_playback",
                            true
                        )
                    ) return hide()
                    if (sharedPreferences.getString("lastPlayed", null)
                            .isNullOrBlank()
                    ) return hide()
                    show()
                }
            }
        }
    }

    companion object {
        val MEDIA_FILE_FILTER = FileFilter { file ->
            if (file.isDirectory) {
                val contents: Array<String> = file.list() ?: arrayOf()
                // filter hidden files due to stuff like ".thumbnails"
                contents.filterNot { it.startsWith('.') }.any()
            } else {
                Utils.MEDIA_EXTENSIONS.contains(file.extension.lowercase())
            }
        }

        fun getThemeColor(context: Context, attr: Int = android.R.attr.colorControlNormal): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attr, typedValue, true)
            return ContextCompat.getColor(context, typedValue.resourceId)
        }
    }
}