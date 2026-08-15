package `is`.xyz.mpv.preferences

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvLogLevel
import `is`.xyz.mpv.R
import `is`.xyz.mpv.databinding.ActivityAboutBinding

/**
 * AboutActivity displays version information and MPV logs
 */
class AboutActivity : AppCompatActivity(), MPVLib.LogObserver {
    
    private lateinit var binding: ActivityAboutBinding
    private val logsBuilder = StringBuilder()
    private var mpvInitialized = false
    private val logLock = Any()
    
    companion object {
        private const val TAG = "AboutActivity"
        private const val MAX_LOGS_LENGTH = 100000 // Prevent unbounded memory growth
    }
    
    // ========== Lifecycle ==========
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setupTheme()
            setupUI()
            initializeLogs()
            initializeMPV()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in onCreate", e)
            showError("Failed to initialize activity: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanupMPV()
    }
    
    // ========== Setup Methods ==========
    
    private fun setupTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("material_you_theming", false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }
    
    private fun setupUI() {
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.elevation = 0f
        
        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Setup logs text view
        binding.logs.text = "Loading..."
    }
    
    private fun initializeLogs() {
        synchronized(logLock) {
            logsBuilder.clear()
            logsBuilder.append("===========================================\n")
            logsBuilder.append("CustomMPV ${BuildConfig.VERSION_NAME}\n")
            logsBuilder.append("Version: ${BuildConfig.VERSION_CODE}\n")
            logsBuilder.append("Build Type: ${BuildConfig.BUILD_TYPE}\n")
            logsBuilder.append("===========================================\n\n")
            logsBuilder.append("Initializing MPV...\n")
        }
        updateLogsUI()
    }
    
    private fun initializeMPV() {
        try {
            // Register observer BEFORE creating MPV to catch all logs
            MPVLib.addLogObserver(this)
            
            // Create and initialize MPV
            MPVLib.create(this)
            mpvInitialized = true
            MPVLib.init()
            
            Log.i(TAG, "MPV initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MPV", e)
            mpvInitialized = false
            
            synchronized(logLock) {
                logsBuilder.append("\n[ERROR] Failed to initialize MPV\n")
                logsBuilder.append("Exception: ${e.javaClass.simpleName}\n")
                logsBuilder.append("Message: ${e.message}\n")
                logsBuilder.append("\nStack Trace:\n")
                logsBuilder.append(e.stackTraceToString())
            }
            updateLogsUI()
        }
    }
    
    private fun cleanupMPV() {
        try {
            // Always remove observer first to prevent callbacks during cleanup
            if (mpvInitialized) {
                MPVLib.removeLogObserver(this)
            }
            
            // Then destroy MPV if it was initialized
            if (mpvInitialized) {
                MPVLib.destroy()
                mpvInitialized = false
                Log.i(TAG, "MPV cleaned up successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    // ========== Log Observer ==========
    
    override fun logMessage(prefix: String, level: Int, text: String) {
        // Only capture cplayer logs
        if (prefix != "cplayer") return
        
        try {
            var shouldUpdate = false
            
            synchronized(logLock) {
                // Append verbose logs
                if (level == MpvLogLevel.MPV_LOG_LEVEL_V) {
                    appendLog(text)
                    shouldUpdate = true
                }
                
                // Always capture feature list
                if (text.startsWith("List of enabled features:", ignoreCase = true)) {
                    appendLog(text)
                    shouldUpdate = true
                }
            }
            
            if (shouldUpdate) {
                updateLogsUI()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logMessage", e)
        }
    }
    
    private fun appendLog(text: String) {
        // Trim logs if they get too large to prevent memory issues
        if (logsBuilder.length > MAX_LOGS_LENGTH) {
            logsBuilder.delete(0, logsBuilder.length / 2)
        }
        
        logsBuilder.append(text)
        if (!text.endsWith("\n")) {
            logsBuilder.append("\n")
        }
    }
    
    // ========== UI Updates ==========
    
    private fun updateLogsUI() {
        // Only update if activity is still alive and not destroyed
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            runOnUiThread {
                try {
                    if (!isDestroyed) {
                        synchronized(logLock) {
                            binding.logs.text = logsBuilder.toString()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating logs UI", e)
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Log.e(TAG, message)
        runOnUiThread {
            if (!isDestroyed) {
                binding.logs.text = "[ERROR]\n\n$message"
            }
        }
    }
}
