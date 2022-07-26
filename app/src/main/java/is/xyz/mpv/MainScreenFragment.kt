package `is`.xyz.mpv

import `is`.xyz.filepicker.DocumentPickerFragment
import `is`.xyz.mpv.config.SettingsActivity
import `is`.xyz.mpv.databinding.FragmentMainScreenBinding
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class MainScreenFragment : Fragment(R.layout.fragment_main_screen) {
    private lateinit var binding: FragmentMainScreenBinding

    private lateinit var documentTreeOpener: ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    private var firstRun = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        documentTreeOpener = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data?.let { root ->
                requireContext().contentResolver.takePersistableUriPermission(
                    root, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveChoice("doc", root.toString())

                val i = Intent(context, FilePickerActivity::class.java)
                i.putExtra("skip", FilePickerActivity.DOC_PICKER)
                i.putExtra("root", root.toString())
                filePickerLauncher.launch(i)
            }
        }
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                Log.v(TAG, "file picker cancelled")
                return@registerForActivityResult
            }
            val path = it.data?.getStringExtra("path")
            if (path != null)
                playFile(path)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentMainScreenBinding.bind(view)

        binding.docBtn.setOnClickListener {
            documentTreeOpener.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
        }
        binding.urlBtn.setOnClickListener {
            saveChoice("url")
            val helper = Utils.OpenUrlDialog()
            with (helper.getBuilder(requireContext())) {
                setPositiveButton(R.string.dialog_ok) { _, _ ->
                    playFile(helper.text)
                }
                setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                show()
            }
        }
        binding.filepickerBtn.setOnClickListener {
            saveChoice("file")
            val i = Intent(context, FilePickerActivity::class.java)
            i.putExtra("skip", FilePickerActivity.FILE_PICKER)
            filePickerLauncher.launch(i)
        }
        binding.settingsBtn.setOnClickListener {
            saveChoice("") // will reset
            startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (firstRun) {
            restoreChoice()
            firstRun = false
        }
    }

    private fun saveChoice(type: String, data: String? = null) {
        if (!binding.switch1.isChecked)
            return
        binding.switch1.isChecked = false
        with (PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()) {
            putString("MainScreenFragment_remember", type)
            if (data == null)
                remove("MainScreenFragment_remember_data")
            else
                putString("MainScreenFragment_remember_data", data)
            commit()
        }
    }

    private fun restoreChoice() {
        val (type, data) = with (PreferenceManager.getDefaultSharedPreferences(requireContext())) {
            Pair(
                getString("MainScreenFragment_remember", "") ?: "",
                getString("MainScreenFragment_remember_data", "") ?: ""
            )
        }

        when (type) {
            "doc" -> {
                val uri = Uri.parse(data)
                // check that we can still access the folder
                if (!DocumentPickerFragment.isTreeUsable(requireContext(), uri))
                    return

                val i = Intent(context, FilePickerActivity::class.java)
                i.putExtra("skip", FilePickerActivity.DOC_PICKER)
                i.putExtra("root", uri.toString())
                filePickerLauncher.launch(i)
            }
            "url" -> binding.urlBtn.callOnClick()
            "file" -> binding.filepickerBtn.callOnClick()
        }
    }

    private fun playFile(filepath: String) {
        val i: Intent
        if (filepath.startsWith("content://")) {
            i = Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
        } else {
            i = Intent()
            i.putExtra("filepath", filepath)
        }
        i.setClass(requireContext(), MPVActivity::class.java)
        startActivity(i)
    }

    companion object {
        private const val TAG = "mpv"
    }
}
