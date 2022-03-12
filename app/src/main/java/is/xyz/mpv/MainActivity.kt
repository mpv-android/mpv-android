package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.ActivityMainBinding
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

class MainActivity : AppCompatActivity() {
    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        materialYouFileExplorer = MaterialYouFileExplorer()

        materialYouFileExplorer.initialize(this)

        binding.bottomNavigationView.apply {
            setOnItemSelectedListener {
                val navController = findNavController(R.id.fragmentContainerView)
                it.onNavDestinationSelected(navController) || super.onOptionsItemSelected(it)
            }
        }
    }

    private fun playFile(filepath: String) {
        val i = Intent(this, MPVActivity::class.java)
        i.putExtra("filepath", filepath)
        startActivity(i)
    }

    private fun selectFileToPlay() {
        materialYouFileExplorer.toExplorer(this, true,
            getString(R.string.action_pick_file), Utils.MEDIA_EXTENSIONS, true) { path, _ -> playFile(path) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickAddFile(view: View?) {
        selectFileToPlay()
    }
}
