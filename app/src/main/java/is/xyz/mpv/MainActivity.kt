package `is`.xyz.mpv

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

import `is`.xyz.filepicker.AbstractFilePickerFragment
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import java.io.File

class MainActivity : AppCompatActivity(), AbstractFilePickerFragment.OnFilePickedListener {

    private var fragment: MPVFilePickerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragment = supportFragmentManager.findFragmentById(R.id.file_picker_fragment) as MPVFilePickerFragment

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val path = sharedPreferences.getString("default_file_manager_path",
                getExternalStorageDirectory().path)
        (fragment as MPVFilePickerFragment).goToDir(File(path))

        // this has absolutely no relation to the file picker, but we need to do it *somewhere*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.pref_background_play_title)
            val ch = NotificationChannel(
                    BackgroundPlaybackService.NOTIFICATION_CHANNEL_ID,
                    name, NotificationManager.IMPORTANCE_MIN)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(ch)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_open_url) {
            // https://stackoverflow.com/questions/10903754/#answer-10904665
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.action_open_url)

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(R.string.dialog_ok) {
                dialog, _ -> playFile(input.text.toString())
            }
            builder.setNegativeButton(R.string.dialog_cancel) {
                dialog, _ -> dialog.cancel()
            }
            builder.show()
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
        private val TAG = "mpv"
    }
}
