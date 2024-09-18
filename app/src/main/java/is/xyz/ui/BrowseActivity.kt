package `is`.xyz.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import `is`.xyz.mpv.FilePickerActivity
import `is`.xyz.mpv.MPVActivity
import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.config.SettingsActivity

class BrowseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_browse)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browse_root)) { v, insets ->
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

        findViewById<MaterialCardView>(R.id.pick_file_button).setOnClickListener {
            val i = Intent(this, FilePickerActivity::class.java)
            i.putExtra("skip", FilePickerActivity.FILE_PICKER)
//            if (lastPath != "")
//                i.putExtra("default_path", lastPath)
            filePickerLauncher.launch(i)
        }

        findViewById<MaterialCardView>(R.id.open_url_button).setOnClickListener {
            val helper = Utils.OpenUrlDialog(this)
            with(helper) {
                builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                    launchPlayer(helper.text)
                }
                builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                create().show()
            }
        }

        findViewById<MaterialCardView>(R.id.open_doc_tree_button).setOnClickListener {
            try {
                documentTreeOpener.launch(null)
            } catch (e: ActivityNotFoundException) {
                // Android TV doesn't come with a document picker and certain versions just throw
                // instead of handling this gracefully
                //binding.docBtn.isEnabled = false
                //TODO Detect this beforehand??
            }
        }

        findViewById<ExtendedFloatingActionButton>(R.id.resume_last_playback).setOnClickListener {
            // TODO
        }

    }


    private fun launchPlayer(filepath: String) {
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
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}