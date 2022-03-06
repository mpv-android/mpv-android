package `is`.xyz.mpv

import `is`.xyz.mpv.config.SettingsActivity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.app.UiModeManager
import android.content.res.Configuration
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

import `is`.xyz.mpv.databinding.ActivityMainBinding
import android.view.MenuItem
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        materialYouFileExplorer = MaterialYouFileExplorer()

        if (PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            materialYouFileExplorer.initialize(this)
            materialYouFileExplorer.toExplorer(this, true) { path, _ -> playFile(path) }
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

        when (id) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }

            R.id.action_external_storage -> {
                materialYouFileExplorer.toExplorer(this, true) { path, _ -> playFile(path) }
                return true
            }
        }
        return false
    }

    private fun playFile(filepath: String) {
        val i = Intent(this, MPVActivity::class.java)
        i.putExtra("filepath", filepath)
        startActivity(i)
    }

    companion object {
        lateinit var materialYouFileExplorer:MaterialYouFileExplorer
    }
}
