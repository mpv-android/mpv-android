package `is`.xyz.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `is`.xyz.mpv.FilePickerActivity
import `is`.xyz.mpv.MPVActivity
import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.databinding.ActivityBrowseBinding

class BrowseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBrowseBinding.inflate(layoutInflater)
        preferences = getSharedPreferences(localClassName, MODE_PRIVATE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode != Activity.RESULT_OK) {
                    return@registerForActivityResult
                }
                it.data?.getStringExtra("last_path")?.let { path ->
//                    lastPath = path
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
//                    saveChoice("doc", root.toString())

                    val i = Intent(this, FilePickerActivity::class.java)
                    i.putExtra("skip", FilePickerActivity.DOC_PICKER)
                    i.putExtra("root", root.toString())
                    filePickerLauncher.launch(i)
                }
            }

        with(binding) {
            pickFileButton.setOnClickListener {
                val i = Intent(this@BrowseActivity, FilePickerActivity::class.java)
                i.putExtra("skip", FilePickerActivity.FILE_PICKER)
//            if (lastPath != "")
//                i.putExtra("default_path", lastPath)
                filePickerLauncher.launch(i)
            }

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

            openDocTreeButton.setOnClickListener {
                try {
                    documentTreeOpener.launch(null)
                } catch (e: ActivityNotFoundException) {
                    // Android TV doesn't come with a document picker and certain versions just throw
                    // instead of handling this gracefully
                    //binding.docBtn.isEnabled = false
                    //TODO Detect this beforehand??
                }
            }

            if (preferences.getString("lastPlayed", null).isNullOrBlank()) {
                resumeLastPlayback.hide()
            }

            resumeLastPlayback.setOnClickListener {
                preferences.getString("lastPlayed", null)?.let { path ->
                    launchPlayer(path)
                }
            }
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, PreferenceActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}