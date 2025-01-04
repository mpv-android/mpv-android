package `is`.xyz.mpv.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import `is`.xyz.mpv.databinding.FragmentFileListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPickerFragment(
    private val mediaHandler: MediaHandler, onPlayMedia: (media: Media) -> Unit,
) : Fragment() {
    private var adapter: FileListAdapter = FileListAdapter(onPlayMedia)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFileListBinding.inflate(inflater)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3) // LinearLayoutManager(context)
            adapter = this@MediaPickerFragment.adapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFilesAsync()
    }

    fun loadFilesAsync(onFinish: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val files = mediaHandler.load()

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
