package `is`.xyz.mpv

import `is`.xyz.mpv.databinding.DialogTrackBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

internal typealias Listener = (MPVView.Track, Boolean) -> Unit

internal class SubTrackDialog(private val player: MPVView) {
    private lateinit var binding: DialogTrackBinding

    private var tracks = listOf<MPVView.Track>()
    private var secondary = false
    // ID of the selected primary track
    private var selectedMpvId = -1
    // ID of the selected secondary track
    private var selectedMpvId2 = -1

    var listener: Listener? = null

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogTrackBinding.inflate(layoutInflater)

        binding.primaryBtn.setOnClickListener {
            secondary = false
            refresh()
        }
        binding.secondaryBtn.setOnClickListener {
            secondary = true
            refresh()
        }

        // Set up recycler view
        binding.list.adapter = CustomAdapter(this)
        refresh()

        return binding.root
    }

    fun refresh() {
        tracks = player.tracks.getValue(TRACK_TYPE)
        selectedMpvId = player.sid
        selectedMpvId2 = player.secondarySid

        // show primary/secondary toggle if applicable
        if (secondary || selectedMpvId2 != -1 || tracks.size > 2) {
            binding.buttonRow.visibility = View.VISIBLE
            binding.divider.visibility = View.VISIBLE
        } else {
            binding.buttonRow.visibility = View.GONE
            binding.divider.visibility = View.GONE
        }

        binding.list.adapter!!.notifyDataSetChanged()
        val index = tracks.indexOfFirst { it.mpvId == if (secondary) selectedMpvId2 else selectedMpvId }
        binding.list.scrollToPosition(index)

        // should fix a layout bug with empty space that happens on api 33
        binding.list.post {
            binding.list.parent.requestLayout()
        }
    }

    private fun clickItem(position: Int) {
        val item = tracks[position]
        listener?.invoke(item, secondary)
    }

    class CustomAdapter(private val parent: SubTrackDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: SubTrackDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: CheckedTextView

            init {
                textView = ViewCompat.requireViewById(view, android.R.id.text1)
                view.setOnClickListener {
                    parent.clickItem(bindingAdapterPosition)
                }
            }

            fun bind(track: MPVView.Track, checked: Boolean, disabled: Boolean) {
                with (textView) {
                    text = track.name
                    isChecked = checked
                    isEnabled = !disabled
                }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dialog_track_item, viewGroup, false)
            return ViewHolder(parent, view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val track = parent.tracks[position]
            var (checked, disabled) = if (parent.secondary) {
                Pair(track.mpvId == parent.selectedMpvId2, track.mpvId == parent.selectedMpvId)
            } else {
                Pair(track.mpvId == parent.selectedMpvId, track.mpvId == parent.selectedMpvId2)
            }
            // selectedMpvId2 may be -1 but this special entry is for disabling a track
            if (track.mpvId == -1)
                disabled = false
            viewHolder.bind(track, checked, disabled)
        }

        override fun getItemCount() = parent.tracks.size
    }

    companion object {
        private const val TAG = "mpv"
        const val TRACK_TYPE = "sub"
    }
}
