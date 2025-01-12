package `is`.xyz.mpv.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.ImageRequest
import coil3.request.target
import coil3.video.VideoFrameDecoder
import com.google.android.material.card.MaterialCardView
import `is`.xyz.mpv.R

class FileListAdapter(private val onPlayMedia: (media: Media) -> Unit) :
    ListAdapter<Media, FileListAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_media_grid, parent, false)
        return FileViewHolder(view, onPlayMedia)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FileViewHolder(itemView: View, private val onPlayMedia: (media: Media) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.media_card)
        private val nameText: TextView = itemView.findViewById(R.id.media_title)
        private val sizeText: TextView = itemView.findViewById(R.id.media_subtitle)
        private val typeIcon: ImageView = itemView.findViewById(R.id.media_thumbnail)

        fun bind(fileItem: Media) {
            nameText.text = fileItem.name
            sizeText.text = formatFileSize(fileItem.size)

            videoThumbnailLoader.enqueue(
                ImageRequest.Builder(typeIcon.context)
                    .data(fileItem.uri)
                    .target(typeIcon).build()
            )

            cardView.setOnClickListener {
                onPlayMedia(fileItem)
            }
        }

        private fun formatFileSize(size: Int): String {
            val kb = size / 1024
            val mb = kb / 1024
            val gb = mb / 1024
            return when {
                gb >= 1 -> String.format("%d GB", gb)
                mb >= 1 -> String.format("%d MB", mb)
                kb >= 1 -> String.format("%d KB", kb)
                else -> String.format("%d Bytes", size)
            }
        }

        private val videoThumbnailLoader = ImageLoader.Builder(itemView.context).diskCache {
            DiskCache.Builder().directory(typeIcon.context.cacheDir.resolve("thumbnails"))
                .maxSizePercent(0.02).build()
        }.components { add(VideoFrameDecoder.Factory()) }.build()

    }
}

// DiffUtil for efficient RecyclerView updates
class FileDiffCallback : DiffUtil.ItemCallback<Media>() {
    override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
        return oldItem.uri == newItem.uri
    }
}