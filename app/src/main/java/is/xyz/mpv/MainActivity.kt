package `is`.xyz.mpv

import `is`.xyz.mpv.config.SettingsActivity
import `is`.xyz.mpv.databinding.ActivityMainBinding
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

class MainActivity : AppCompatActivity() {
    lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        materialYouFileExplorer = MaterialYouFileExplorer()

        materialYouFileExplorer.initialize(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }

            R.id.action_external_storage -> {
                selectFileToPlay()
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

    private fun selectFileToPlay() {
        materialYouFileExplorer.toExplorer(this, true) { path, _ -> playFile(path) }
    }
}
