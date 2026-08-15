package `is`.xyz.mpv.preferences

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvLogLevel
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity(), MPVLib.LogObserver {
    private lateinit var binding: ActivityAboutBinding
    
    // Use StringBuilder for thread-safe and efficient string concatenation
    private val logsBuilder = StringBuilder()
    private var mpvDestroyed = true
    private val logLock = Any() // Lock for synchronization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("material_you_theming", false))
            DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.elevation = 0f
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        synchronized(logLock) {
            logsBuilder.append("CustomMPV ${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n")
        }

        // create mpv context to capture version info from log
        // NOTE: Be cautious with MPVLib.create/destroy if a video is currently playing elsewhere in the app.
        MPVLib.create(this)
        mpvDestroyed = false
        MPVLib.addLogObserver(this)
        MPVLib.init()
    }

    private fun updateLog() {
        runOnUiThread {
            synchronized(logLock) {
                binding.logs.text = logsBuilder.toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // CRITICAL FIX: Always remove the observer to prevent memory leaks and crashes 
        // if the activity is destroyed before the "List of enabled features:" log appears.
        MPVLib.removeLogObserver(this)

        if (!mpvDestroyed) {
            MPVLib.destroy()
            mpvDestroyed = true
        }
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer") return

        var isTrigger = false
        
        synchronized(logLock) {
            // FIX: Append text if it is Verbose OR if it is the target trigger string
            if (level == MpvLogLevel.MPV_LOG_LEVEL_V || text.startsWith("List of enabled features:", true)) {
                logsBuilder.append(text)
            }

            if (text.startsWith("List of enabled features:", true)) {
                isTrigger = true
            }
        }

        if (isTrigger) {
            // stop receiving log messages and populate text field
            MPVLib.removeLogObserver(this)
            updateLog()
        }
    }
}
