package `is`.xyz.mpv.browse

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import `is`.xyz.mpv.BuildConfig
import `is`.xyz.mpv.MPVActivity
import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.databinding.ActivityBrowseBinding
import `is`.xyz.mpv.preferences.PreferenceActivity

class BrowseActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityBrowseBinding
    private lateinit var preferences: SharedPreferences
    private lateinit var fragment: MediaPickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        if (preferences.getBoolean("material_you_theming", false)) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {

            openUrlButton.setOnClickListener {
                val helper = Utils.OpenUrlDialog(this@BrowseActivity)
                with(helper) {
                    builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                        launchPlayer(helper.text)
                    }
                    builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                    create().show()
                }
            }

            resumeLastPlayback.setOnClickListener {
                preferences.getString("lastPlayed", null)?.let { path ->
                    launchPlayer(path)
                }
            }

            swipeRefresh.setOnRefreshListener {
                if (::fragment.isInitialized) fragment.loadFilesAsync(MediaHandler(this@BrowseActivity)) {
                    swipeRefresh.isRefreshing = false
                }
            }

            if (BuildConfig.DEBUG) {
                binding.toolbar.setOnLongClickListener { showDebugMenu(); true }
            }

            initFilePicker()
        }

        onBackPressedDispatcher.addCallback(this) {
//            if (::fragment.isInitialized && !fragment.isBackTop) {
//                fragment.goUp()
//                return@addCallback
//            }
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val hasResumeData = preferences.getBoolean(
            "remember_last_playback", true
        ) && preferences.getString("lastPlayed", "")!!.isNotBlank()

        if (hasResumeData) {
            binding.resumeLastPlayback.show()
            binding.openUrlButton.size = FloatingActionButton.SIZE_MINI
        } else {
            binding.resumeLastPlayback.hide()
            binding.openUrlButton.size = FloatingActionButton.SIZE_NORMAL
        }
    }

    private fun initFilePicker() {
        if (!::fragment.isInitialized) {
            binding.swipeRefresh.isRefreshing = true
            fragment = MediaPickerFragment()

            with(supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(binding.fragmentContainer.id, fragment, null)
                commit()
            }

            fragment.loadFilesAsync(MediaHandler(this)) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }


    private fun showDebugMenu() {
        assert(BuildConfig.DEBUG)
        with(MaterialAlertDialogBuilder(this)) {
            setItems(DEBUG_ACTIVITIES) { dialog, idx ->
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_MAIN)
                intent.setClassName(context, "${context.packageName}.${DEBUG_ACTIVITIES[idx]}")
                startActivity(intent)
            }
            create().show()
        }
    }

    private fun launchPlayer(filepath: String) {
        preferences.edit().putString("lastPlayed", filepath).apply()
        val i: Intent = when {
            filepath.startsWith("content://") -> Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
            else -> Intent().putExtra("filepath", filepath)
        }.setClass(this, MPVActivity::class.java)
        startActivity(i)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_browse, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, PreferenceActivity::class.java))
                return true
            }

            R.id.action_file_filter -> {
                fragment.showFilterDialog()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!::fragment.isInitialized) return
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            binding.swipeRefresh.isRefreshing = true
            fragment.loadFilesAsync(MediaHandler(this)) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "material_you_theming" -> {
                if (sharedPreferences.getBoolean(key, false)) {
                    DynamicColors.applyToActivityIfAvailable(this)
                }
                recreate()
            }
        }
    }

    companion object {
        private val DEBUG_ACTIVITIES = arrayOf(
            "IntentTestActivity"
        )
    }
}