package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.DialogPlaylistBinding
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

internal class PlaylistDialog(private val player: MPVView) {
    private lateinit var binding: DialogPlaylistBinding

    private var playlist = listOf<MPVView.PlaylistItem>()
    private var selectedIndex = -1

    private var pickFileAction: View.OnClickListener? = null
    private var openUrlAction: View.OnClickListener? = null
    private var pickItemListener: ((MPVView.PlaylistItem) -> Unit)? = null

    fun setPickFileAction(listener: View.OnClickListener) { pickFileAction = listener }
    fun setOpenUrlAction(listener: View.OnClickListener) { openUrlAction = listener }
    fun setPickItemListener(listener: (MPVView.PlaylistItem) -> Unit) { pickItemListener = listener }

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogPlaylistBinding.inflate(layoutInflater)

        // Set up recycler view
        binding.list.adapter = CustomAdapter(this)
        binding.list.setHasFixedSize(true)
        loadPlaylist()

        binding.fileBtn.setOnClickListener(pickFileAction)
        binding.urlBtn.setOnClickListener(openUrlAction)

        return binding.root
    }

    fun loadPlaylist() {
        selectedIndex = MPVLib.getPropertyInt("playlist-pos") ?: -1
        playlist = player.loadPlaylist()
        Log.v(TAG, "PlaylistDialog: loaded ${playlist.size} items")
        (binding.list.adapter as RecyclerView.Adapter).notifyDataSetChanged()
        binding.list.scrollToPosition(playlist.indexOfFirst { it.index == selectedIndex })
    }

    private fun clickItem(position: Int) {
        val item = playlist[position]
        pickItemListener?.invoke(item)
    }

    class CustomAdapter(private val parent: PlaylistDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: PlaylistDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: TextView
            var selfPosition: Int = -1

            init {
                textView = view.findViewById(R.id.text)
                view.setOnClickListener {
                    parent.clickItem(selfPosition)
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
            viewHolder.selfPosition = position
            val item = parent.playlist[position]
            viewHolder.bind(item, item.index == parent.selectedIndex)
        }

        override fun getItemCount() = parent.playlist.size
    }

    companion object {
        private const val TAG = "mpv"
    }
}
