package `is`.xyz.preference

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.ActivityAboutBinding


class AboutActivity : AppCompatActivity(), MPVLib.LogObserver {
    private lateinit var binding: ActivityAboutBinding
    private var logs: String =
        "mpv-android ${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n"

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("material_you_theming", false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.about_mpv)
        supportActionBar?.elevation = 0F
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /* create mpv context to capture version info from log */
        MPVLib.create(this)
        MPVLib.addLogObserver(this)
        MPVLib.init()

    }

    private fun updateLog() {
        runOnUiThread {
            binding.logs.text = logs
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MPVLib.destroy()
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer") return
        if (level == MPVLib.mpvLogLevel.MPV_LOG_LEVEL_V) logs += text

        if (text.startsWith("List of enabled features:")) {
            /* stop receiving log messages and populate text field */
            MPVLib.removeLogObserver(this)
            updateLog()
        }
    }
}