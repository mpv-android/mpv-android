package `is`.xyz.mpv

import `is`.xyz.filepicker.AbstractFilePickerFragment
import android.content.Intent
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.preference.PreferenceManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FilePickerActivity : AppCompatActivity(), AbstractFilePickerFragment.OnFilePickedListener {
    private var fragment: MPVFilePickerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "FilePickerActivity: created")
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_filepicker)
        supportActionBar?.hide()

        // Hide everything for now
        findViewById<View>(android.R.id.content).visibility = View.GONE

        when (intent.getIntExtra("skip", -1)) {
            URL_DIALOG -> {
                showUrlDialog()
                return
            }
            FILE_PICKER -> {
                initFilePicker()
                return
            }
        }

        // Figure out what the user wants
        lateinit var askDialog: AlertDialog
        val view = layoutInflater.inflate(R.layout.dialog_filepicker, null)
        view.findViewById<TextView>(R.id.fileBtn).setOnClickListener {
            askDialog.dismiss()
            initFilePicker()
        }
        view.findViewById<TextView>(R.id.urlBtn).setOnClickListener {
            askDialog.dismiss()
            showUrlDialog()
        }

        askDialog = with (AlertDialog.Builder(this)) {
            setTitle(intent.getStringExtra("title"))
            setView(view)
            setOnCancelListener { finishWithResult(RESULT_CANCELED) }
            create()
        }
        askDialog.show()
    }

    private fun initFilePicker() {
        Log.v(TAG, "FilePickerActivity: showing file picker")
        var path = intent.getStringExtra("default_path")
        if (path == null) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            path = sharedPrefs.getString("default_file_manager_path",
                    getExternalStorageDirectory().path)
        }

        // Create fragment
        fragment = MPVFilePickerFragment().also {
            it.goToDir(File(path))
        }
        with (supportFragmentManager.beginTransaction()) {
            add(R.id.file_picker_fragment, fragment!!, null)
            commit()
        }

        // Open the curtains
        findViewById<View>(android.R.id.content).visibility = View.VISIBLE
    }

    private fun showUrlDialog() {
        Log.v(TAG, "FilePickerActivity: showing url dialog")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        with (AlertDialog.Builder(this)) {
            setTitle(R.string.action_open_url)
            setView(input)
            setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                finishWithResult(RESULT_OK, input.text.toString())
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnCancelListener { finishWithResult(RESULT_CANCELED) }
            show()
        }
    }

    override fun onBackPressed() {
        finishWithResult(RESULT_CANCELED)
    }

    private fun finishWithResult(code: Int, path: String? = null) {
        if (path != null) {
            val result = Intent()
            result.putExtra("path", path)
            setResult(code, result)
            Log.v(TAG, "FilePickerActivity: file picked \"$path\"")
        } else {
            setResult(code)
        }
        finish()
    }

    override fun onFilePicked(file: File) = finishWithResult(RESULT_OK, file.absolutePath)

    override fun onDirPicked(dir: File) = finishWithResult(RESULT_OK, dir.absolutePath)

    override fun onCancelled() = finishWithResult(RESULT_CANCELED)

    companion object {
        private const val TAG = "mpv"

        // values for "skip" in intent
        const val URL_DIALOG = 0
        const val FILE_PICKER = 1
    }
}
