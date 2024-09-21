package `is`.xyz.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import `is`.xyz.mpv.R


class PreferenceActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val frameLayout = FrameLayout(this).apply {
            id = R.id.root
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.id = R.id.root
        setContentView(frameLayout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0F
        ViewCompat.setOnApplyWindowInsetsListener(frameLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportFragmentManager.beginTransaction().replace(R.id.root, SettingsFragment()).commit()
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }

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

        supportFragmentManager.beginTransaction()
            .replace(R.id.root, fragment)
            .addToBackStack(null)
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
            setPreferencesFromResource(R.xml.pref_root, rootKey)
        }
    }


    class GeneralPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_general, rootKey)
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

