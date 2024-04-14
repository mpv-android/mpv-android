package `is`.xyz.mpv

import `is`.xyz.filepicker.AbstractFilePickerFragment
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import `is`.xyz.filepicker.FilePickerFragment
import `is`.xyz.mpv.databinding.FragmentFilepickerChoiceBinding
import java.io.File
import java.io.FileFilter

class FilePickerActivity : AppCompatActivity(), AbstractFilePickerFragment.OnFilePickedListener {
    private var fragment: MPVFilePickerFragment? = null
    private var fragment2: MPVDocumentPickerFragment? = null

    private var lastSeenInsets: WindowInsets? = null

    private var documentOpener = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri ->
            finishWithResult(RESULT_OK, uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        Log.v(TAG, "FilePickerActivity: created")

        setContentView(R.layout.activity_filepicker)
        supportActionBar?.title = ""

        // The basic issue we have here is this: https://stackoverflow.com/questions/31190612/
        // Some part of the view hierachy swallows the insets during fragment transitions
        // and it's impossible to invoke this calculation a second time (requestApplyInsets doesn't help).
        // For that reason I wrote this creative workaround, it works surprisingly well.
        findViewById<View>(R.id.fragment_container_view).setOnApplyWindowInsetsListener { _, insets ->
            lastSeenInsets = WindowInsets(insets)
            insets
        }

        when (intent.getIntExtra("skip", -1)) {
            URL_DIALOG -> {
                showUrlDialog()
                return
            }
            FILE_PICKER -> {
                initFilePicker()
                return
            }
            DOC_PICKER -> {
                val root = Uri.parse(intent.getStringExtra("root")!!)
                initDocPicker(root)
                return
            }
        }

        // Ask the user what he wants
        val args = Bundle().apply {
            putString("title", intent.getStringExtra("title"))
            putBoolean("allow_document", intent.getBooleanExtra("allow_document", false))
        }
        with (supportFragmentManager.beginTransaction()) {
            setReorderingAllowed(true)
            add(R.id.fragment_container_view, ChoiceFragment::class.java, args, null)
            commit()
        }
    }

    private fun doUiTweaks() {
        // Set the system UI to act as if the nav bar is hidden, so that we can
        // draw behind it. STABLE flag is historically recommended but was
        // deprecated in API level 30, so probably not strictly necessary, but
        // cargo-culting is fun.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        // Part 2 of the workaround: apply the insets to the recycler so it can
        // take them into account.
        val recycler: RecyclerView = findViewById(android.R.id.list)
        lastSeenInsets?.let { recycler.onApplyWindowInsets(lastSeenInsets) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (fragment == null)
            return
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // re-init file picker with correct paths
            initFilePicker()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (fragment == null) // no menu in doc picker mode
            return true
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION)
            menuInflater.inflate(R.menu.menu_filepicker, menu)
        else
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "...") // dummy menu item to indicate presence
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_external_storage -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    val path = Environment.getExternalStorageDirectory()
                    fragment!!.goToDir(path) // attempt to do something useful
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
            }
            R.id.action_file_filter -> {
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
                    this.putBoolean("${PREF_PREFIX}filter_state", !old)
                    apply()
                }
                return true
            }
            else -> return false
        }
    }

    private fun initFilePicker() {
        // Create fragment first
        if (fragment == null) {
            fragment = MPVFilePickerFragment()
            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, fragment!!, null)
                runOnCommit { doUiTweaks() }
                commit()
            }
        }

        if (!FilePickerFragment.hasPermission(this, File("/"))) {
            Log.v(TAG, "FilePickerActivity: waiting for file picker permission")
            return
        }

        Log.v(TAG, "FilePickerActivity: showing file picker")
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (sharedPrefs.getBoolean("${PREF_PREFIX}filter_state", false)) {
            fragment!!.filterPredicate = MEDIA_FILE_FILTER
        }

        var defaultPathStr = intent.getStringExtra("default_path")
        if (defaultPathStr.isNullOrEmpty()) {
            // TODO: rework or remove this setting
            defaultPathStr = sharedPrefs.getString("default_file_manager_path",
                Environment.getExternalStorageDirectory().path)
        }
        val defaultPath = File(defaultPathStr!!)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // check that the preferred path is inside a storage volume
            val vols = Utils.getStorageVolumes(this)
            val vol = vols.find { defaultPath.startsWith(it.path) }
            if (vol == null) {
                // looks like it wasn't
                Log.w(TAG, "default path set to \"$defaultPath\" but no such storage volume")
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
        } else {
            // Old device: go to preferred path but don't restrict root
            fragment!!.goToDir(defaultPath)
        }
    }

    override fun dispatchKeyEvent(ev: KeyEvent?): Boolean {
        // If up is pressed at the header element display the usual options menu as a popup menu
        // to make it usable on Android TV.
        var openMenu = false
        if (fragment == null) {
            // only for file picker
        } else if (ev?.action == KeyEvent.ACTION_DOWN && ev.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
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
                    this@FilePickerActivity.onOptionsItemSelected(it)
                }
                inflate(R.menu.menu_filepicker)
                show()
            }
            return true
        }

        return super.dispatchKeyEvent(ev)
    }

    private fun initDocPicker(root: Uri) {
        Log.v(TAG, "FilePickerActivity: showing document picker at \"$root\"")
        assert(fragment2 == null)
        fragment2 = MPVDocumentPickerFragment(root)

        val defaultPathStr = intent.getStringExtra("default_path")
        if (!defaultPathStr.isNullOrEmpty()) {
            fragment2!!.apply {
                goToDir(pathFromString(defaultPathStr))
            }
        }

        with (supportFragmentManager.beginTransaction()) {
            setReorderingAllowed(true)
            add(R.id.fragment_container_view, fragment2!!, null)
            runOnCommit { doUiTweaks() }
            commit()
        }
    }

    private fun showUrlDialog() {
        Log.v(TAG, "FilePickerActivity: showing url dialog")
        val helper = Utils.OpenUrlDialog(this)
        with (helper) {
            builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                finishWithResult(RESULT_OK, helper.text)
            }
            builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            builder.setOnCancelListener { finishWithResult(RESULT_CANCELED) }
            create().show()
        }
    }

    override fun onBackPressed() {
        fragment?.apply {
            if (!isBackTop) {
                goUp()
                return
            }
        }
        fragment2?.apply {
            if (!isBackTop) {
                goUp()
                return
            }
        }
        finishWithResult(RESULT_CANCELED)
    }

    private fun finishWithResult(code: Int, path: String? = null) {
        val result = Intent()
        fragment?.apply {
            result.putExtra("last_path", pathToString(currentDir))
        }
        fragment2?.apply {
            result.putExtra("last_path", pathToString(currentDir))
        }
        if (path != null) {
            result.putExtra("path", path)
            Log.v(TAG, "FilePickerActivity: picked \"$path\"")
        } else {
            Log.v(TAG, "FilePickerActivity: nothing picked")
        }
        setResult(code, result)
        finish()
    }

    override fun onFilePicked(file: File) = finishWithResult(RESULT_OK, file.absolutePath)

    override fun onDirPicked(dir: File) = finishWithResult(RESULT_OK, dir.absolutePath)

    override fun onDocumentPicked(uri: Uri, isDir: Boolean) {
        assert(fragment2 != null)
        if (!isDir)
            finishWithResult(RESULT_OK, fragment2!!.pathToString(uri))
    }

    override fun onCancelled() = finishWithResult(RESULT_CANCELED)

    class ChoiceFragment : Fragment(R.layout.fragment_filepicker_choice) {
        private lateinit var binding: FragmentFilepickerChoiceBinding

        private fun removeMyself() {
            with (requireActivity().supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                remove(this@ChoiceFragment)
                commit()
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            binding = FragmentFilepickerChoiceBinding.bind(view)

            binding.message.text = requireArguments().getString("title")
            binding.fileBtn.setOnClickListener {
                removeMyself()
                (activity as FilePickerActivity).initFilePicker()
            }
            binding.urlBtn.setOnClickListener {
                // leave visible, dialog will exit anyway
                (activity as FilePickerActivity).showUrlDialog()
            }
            binding.docBtn.setOnClickListener {
                (activity as FilePickerActivity).documentOpener.launch(arrayOf("*/*"))
            }
            if (!requireArguments().getBoolean("allow_document", false))
                binding.docBtn.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "mpv"

        // legacy leftover
        private const val PREF_PREFIX = "MainActivity_"

        private val MEDIA_FILE_FILTER = FileFilter { file ->
            if (file.isDirectory) {
                val contents: Array<String> = file.list() ?: arrayOf()
                // filter hidden files due to stuff like ".thumbnails"
                contents.filterNot { it.startsWith('.') }.any()
            } else {
                Utils.MEDIA_EXTENSIONS.contains(file.extension.lowercase())
            }
        }

        // values for "skip" in intent
        const val URL_DIALOG = 0
        const val FILE_PICKER = 1
        const val DOC_PICKER = 2
    }
}
