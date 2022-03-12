package `is`.xyz.mpv.config

import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MainActivity
import `is`.xyz.mpv.R
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat(), MPVLib.LogObserver {
    init {
        handler = this
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        (findPreference<Preference>("about_info")?.apply {
            setOnPreferenceClickListener { _->
                doLog()
            }
        })
    }

    private fun doLog() : Boolean {
        if (messageComplete)
            return displayVersionInfo(versionText)
        MPVLib.create(context)
        MPVLib.addLogObserver(handler)
        MPVLib.init()
        libInited = true
        return true
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer")
            return
        if (level == MPVLib.mpvLogLevel.MPV_LOG_LEVEL_V)
            versionText += text
        if (text.startsWith("List of enabled features:")) {
            /* stop receiving log messages and populate text field */
            MPVLib.removeLogObserver(this)
            displayVersionInfo(versionText)
            messageComplete = true
        }
    }

    private fun displayVersionInfo(info:String) : Boolean {
        (context as MainActivity).runOnUiThread {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_advanced_version)
                .setMessage(info)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if(libInited) MPVLib.destroy()
                        libInited = false}
                .create()
                .show()
        }
        return true
    }

    companion object {
        lateinit var handler: SettingsFragment
        var versionText = "mpv-android ${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n"
        var messageComplete = false
        public var libInited = false
    }
}