package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.DialogPlaylistBinding
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DiffUtil

internal class PlaylistDialog(private val player: MPVView) {
    private lateinit var binding: DialogPlaylistBinding

    private var fullPlaylist = listOf<MPVView.PlaylistItem>()
    private var playlist = listOf<MPVView.PlaylistItem>()
    private var selectedIndex = -1

    private val handler = Handler(Looper.getMainLooper())
    private var filterRunnable: Runnable? = null

    interface Listeners {
        fun pickFile()
        fun openUrl()
        fun onItemPicked(item: MPVView.PlaylistItem)
    }

    var listeners: Listeners? = null

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogPlaylistBinding.inflate(layoutInflater)

        // Set up recycler view
        binding.list.adapter = CustomAdapter(this)
        binding.list.setHasFixedSize(true)
        refresh()

        binding.fileBtn.setOnClickListener { listeners?.pickFile() }
        binding.urlBtn.setOnClickListener { listeners?.openUrl() }

        binding.shuffleBtn.setOnClickListener {
            player.changeShuffle(true)
            refresh()
        }
        binding.repeatBtn.setOnClickListener {
            player.cycleRepeat()
            refresh()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRunnable?.let { handler.removeCallbacks(it) }
                filterRunnable = Runnable { filter(newText) }
                handler.postDelayed(filterRunnable!!, 200)
                return true
            }
        })

        // don't go full-screen in landscape
        binding.searchView.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        binding.searchView.post {
            binding.searchView.clearFocus()
        }

        Utils.handleInsetsAsPadding(binding.root)
        return binding.root
    }

    private fun filter(query: String?) {
        val newPlaylist = if (query.isNullOrBlank()) {
            fullPlaylist
        } else {
            val queryParts = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            fullPlaylist.filter { item ->
                queryParts.all { part ->
                    (item.title?.contains(part, ignoreCase = true) ?: false) ||
                    item.filename.contains(part, ignoreCase = true)
                }
            }
        }

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = playlist.size
            override fun getNewListSize(): Int = newPlaylist.size
            override fun areItemsTheSame(oldItemPos: Int, newItemPos: Int): Boolean =
                playlist[oldItemPos].index == newPlaylist[newItemPos].index
            override fun areContentsTheSame(oldItemPos: Int, newItemPos: Int): Boolean =
                playlist[oldItemPos] == newPlaylist[newItemPos]
        })
        playlist = newPlaylist
        diffResult.dispatchUpdatesTo(binding.list.adapter!!)
    }

    fun refresh() {
        selectedIndex = MPVLib.getPropertyInt("playlist-pos") ?: -1
        fullPlaylist = player.loadPlaylist()

        filterRunnable?.let { handler.removeCallbacks(it) }
        filter(binding.searchView.query?.toString())

        Log.v(TAG, "PlaylistDialog: loaded ${fullPlaylist.size} items")
        binding.list.scrollToPosition(playlist.indexOfFirst { it.index == selectedIndex })

        /*
         * At least on api 33 there is in some cases a (reproducible) bug, where the space below the
         * recycler view for the two buttons is not taken into account and they go out-of-bounds of the
         * alert dialog. This fixes it.
         */
        binding.list.post {
            binding.list.parent.requestLayout()
        }

        val accent = ContextCompat.getColor(binding.root.context, R.color.accent)
        val disabled = ContextCompat.getColor(binding.root.context, R.color.alpha_disabled)
        //
        val shuffleState = player.getShuffle()
        binding.shuffleBtn.apply {
            isEnabled = fullPlaylist.size > 1
            imageTintList = if (isEnabled)
                if (shuffleState) ColorStateList.valueOf(accent) else null
            else
                ColorStateList.valueOf(disabled)
        }
        val repeatState = player.getRepeat()
        binding.repeatBtn.apply {
            imageTintList = if (repeatState > 0) ColorStateList.valueOf(accent) else null
            setImageResource(if (repeatState == 2) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp)
        }
    }

    private fun clickItem(position: Int) {
        val item = playlist[position]
        listeners?.onItemPicked(item)
    }

    class CustomAdapter(private val parent: PlaylistDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: PlaylistDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: TextView

            init {
                textView = view.findViewById(android.R.id.text1)
                view.setOnClickListener {
                    parent.clickItem(bindingAdapterPosition)
                }
            }

            fun bind(item: MPVView.PlaylistItem, selected: Boolean) {
                textView.text = item.title ?: Utils.fileBasename(item.filename)
                textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dialog_playlist_item, viewGroup, false)
            return ViewHolder(parent, view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val item = parent.playlist[position]
            viewHolder.bind(item, item.index == parent.selectedIndex)
        }

        override fun getItemCount() = parent.playlist.size
    }

    companion object {
        private const val TAG = "mpv"
    }
}
