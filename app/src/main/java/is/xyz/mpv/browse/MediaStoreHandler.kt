package `is`.xyz.mpv.browse

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.MergeCursor
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch


data class Media(
    val uri: Uri,
    val name: String,
    val duration: Int,
    val size: Int,
    val relativePath: String,
    val absolutePath: String,
)

class MediaHandler(val activity: BrowseActivity) {
    private val metadataRetriever = MediaMetadataRetriever()

    @SuppressLint("InlinedApi")
    private val permissions = listOfNotNull(
        Manifest.permission.READ_MEDIA_AUDIO.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
        Manifest.permission.READ_MEDIA_VIDEO.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
        Manifest.permission.READ_EXTERNAL_STORAGE.takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU },
    )

    init {
        requestNecessaryPermission()
    }

    fun scanAndLoad(): MutableList<Media> {
        val paths = ContextCompat.getExternalFilesDirs(activity, null).map { it.path }

        val countDownLatch = CountDownLatch(1)
        MediaScannerConnection.scanFile(
            activity, paths.toTypedArray(), arrayOf("video/*")
        ) { _, _ -> countDownLatch.countDown() }

        countDownLatch.await()
        return load()
    }

    private fun load(): MutableList<Media> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATA
        )

        @SuppressLint("InlinedApi") val collections =
            MergeCursor(arrayOf(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_INTERNAL)
                .takeIf {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                }, MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL).takeIf {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }, MediaStore.Video.Media.INTERNAL_CONTENT_URI.takeIf {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            }, MediaStore.Video.Media.EXTERNAL_CONTENT_URI.takeIf {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            }).filterNotNull().map {
                activity.contentResolver.query(it, projection, null, null, null)
            }.toTypedArray()
            )


        val medias = mutableListOf<Media>()
        collections.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val relativePathColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val absolutePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)



            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val relativePath = cursor.getString(relativePathColumn)
                val absolutePath = cursor.getString(absolutePathColumn)

                val uri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )


                medias += Media(uri, name, duration, size, relativePath, absolutePath)
            }
        }

        return medias
    }

    private fun requestNecessaryPermission() {
        val neededPermissions = mutableListOf<String>()
        for (permission in permissions) {
            val status = ContextCompat.checkSelfPermission(activity, permission)
            if (PackageManager.PERMISSION_GRANTED != status) neededPermissions += permission
        }
        if (neededPermissions.isEmpty()) return
        ActivityCompat.requestPermissions(activity, neededPermissions.toTypedArray(), 0)
    }

}