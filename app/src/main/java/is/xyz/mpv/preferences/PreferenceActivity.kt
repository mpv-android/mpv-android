package `is`.xyz.mpv.preferences

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.color.DynamicColors
import `is`.xyz.mpv.R


class PreferenceActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        if (preferences.getBoolean("material_you_theming", false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val frameLayout = FrameLayout(this).apply {
            id = R.id.main
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.id = R.id.main
        setContentView(frameLayout)
        supportActionBar?.elevation = 0F
        ViewCompat.setOnApplyWindowInsetsListener(frameLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState != null) {
            title = savedInstanceState.getCharSequence("title")
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.main, SettingsFragment())
                .commit()
            supportFragmentManager.addOnBackStackChangedListener {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    setTitle(R.string.title_activity_settings)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("title", title)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key != "material_you_theming") return
        if (sharedPreferences.getBoolean(key, false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
        recreate()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat, pref: Preference
    ): Boolean {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader, pref.fragment ?: return false
        ).apply { arguments = pref.extras }

        supportFragmentManager.beginTransaction().replace(R.id.main, fragment).addToBackStack(null)
            .commit()

        title = pref.title
        return true
    }

    /**
     * The root preference fragment that displays preferences that link to the other preference
     * fragments below.
     */
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey)
        }
    }


    class GeneralPreference : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_general, rootKey)
            // Hide Material You on Android 11 or lower
            preferenceManager.findPreference<SwitchPreferenceCompat>("material_you_theming")?.isVisible =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            if (newValue == true) DynamicColors.applyToActivityIfAvailable(requireActivity())
            requireActivity().recreate()
            return true
        }


    }


    class VideoPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_video, rootKey)
        }
    }


    class UIPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_ui, rootKey)
        }
    }


    class GesturePreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_gestures, rootKey)
        }
    }


    class DeveloperPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_developer, rootKey)
        }
    }


    class AdvancePreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_advanced, rootKey)
        }
    }
}
