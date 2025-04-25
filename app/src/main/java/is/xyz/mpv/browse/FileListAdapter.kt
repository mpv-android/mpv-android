package `is`.xyz.mpv.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.ImageRequest
import coil3.request.target
import coil3.video.VideoFrameDecoder
import `is`.xyz.mpv.databinding.ItemMediaListBinding
import java.util.Locale

class FileListAdapter(private val onPlayMedia: (media: Media) -> Unit) :
    ListAdapter<Media, FileListAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemMediaListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false,
        )
        return FileViewHolder(binding, onPlayMedia)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FileViewHolder(
        private val binding: ItemMediaListBinding,
        private val onPlayMedia: (media: Media) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(media: Media) {
            binding.mediaTitle.text = media.name
            binding.mediaSubtitle.text = formatFileSize(media.size)

            videoThumbnailLoader.enqueue(
                ImageRequest.Builder(binding.mediaThumbnail.context).data(media.uri)
                    .target(binding.mediaThumbnail).build()
            )

            binding.mediaCard.setOnClickListener { onPlayMedia(media) }
        }

        private fun formatFileSize(size: Int): String {
            val kb = size / 1024
            val mb = kb / 1024
            val gb = mb / 1024
            return when {
                gb >= 1 -> String.format(Locale.ENGLISH, "%d GB", gb)
                mb >= 1 -> String.format(Locale.ENGLISH, "%d MB", mb)
                kb >= 1 -> String.format(Locale.ENGLISH, "%d KB", kb)
                else -> String.format(Locale.ENGLISH, "%d Bytes", size)
            }
        }

        private val videoThumbnailLoader = ImageLoader.Builder(itemView.context).diskCache {
            DiskCache.Builder()
                .directory(binding.mediaThumbnail.context.cacheDir.resolve("thumbnails"))
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