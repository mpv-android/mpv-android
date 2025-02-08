package `is`.xyz.mpv.browse

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import `is`.xyz.mpv.MPVActivity
import `is`.xyz.mpv.databinding.FragmentFileListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPickerFragment : Fragment() {

    private lateinit var preferences: SharedPreferences
    private lateinit var adapter: FileListAdapter

    private fun launchPlayer(filepath: String) {
        preferences.edit().putString("lastPlayed", filepath).apply()
        val i: Intent = when {
            filepath.startsWith("content://") -> Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
            else -> Intent().putExtra("filepath", filepath)
        }.setClass(requireContext(), MPVActivity::class.java)
        startActivity(i)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        val binding = FragmentFileListBinding.inflate(inflater)
        adapter = FileListAdapter {
            launchPlayer(it.uri.toString())
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context) // LinearLayoutManager(context)
            adapter = this@MediaPickerFragment.adapter
        }

        loadFilesAsync(MediaHandler(requireActivity()))

        return binding.root
    }

    fun loadFilesAsync(
        mediaHandler: MediaHandler,
        onFinish: (() -> Unit)? = null,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val files = mediaHandler.scanAndLoad()

            withContext(Dispatchers.Main) {
                adapter.submitList(files)
                if (onFinish != null) onFinish()
            }
        }
    }

    fun showFilterDialog() {
        // TODO
    }
}
