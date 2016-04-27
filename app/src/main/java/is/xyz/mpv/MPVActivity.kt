package `is`.xyz.mpv

import kotlinx.android.synthetic.main.player.*

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.content.res.AssetManager
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.content.Intent
import android.net.Uri
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import kotlinx.android.synthetic.main.player.view.*

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MPVActivity : Activity(), EventObserver {

    lateinit internal var fadeHandler: Handler
    lateinit internal var fadeRunnable: FadeOutControlsRunnable

    internal var userIsOperatingSeekbar = false

    lateinit internal var toast: Toast

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser)
                return
            player.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    private fun initListeners() {
        controls.cycleAudioBtn.setOnClickListener { v ->  cycleAudio() }
        controls.cycleAudioBtn.setOnLongClickListener { v -> pickAudio(); true }

        controls.cycleSubsBtn.setOnClickListener { v ->cycleSub() }
        controls.cycleSubsBtn.setOnLongClickListener { v -> pickSub(); true }
    }

    private fun initMessageToast() {
        toast = makeText(applicationContext, "This totally shouldn't be seen", LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // Do copyAssets here and not in MainActivity because mpv can be launched from a file browser
        copyAssets()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.player)

        // Init controls to be hidden and view fullscreen
        initControls()

        // Initialize listeners for the player view
        initListeners()

        // Initialize toast used for short messages
        initMessageToast()

        // set up a callback handler and a runnable for fading the controls out
        fadeHandler = Handler()
        fadeRunnable = FadeOutControlsRunnable(this, controls)

        var filepath: String?
        if (intent.action == Intent.ACTION_VIEW) {
            // launched as viewer for a specific file
            val data = intent.data
            filepath = when (data.scheme) {
                "file" -> data.path
                "content" -> getRealPathFromURI(data)
                "http" -> data.toString()
                else -> null
            }

            if (filepath == null)
                Log.e(TAG, "unknown scheme: ${data.scheme}")
        } else {
            filepath = intent.getStringExtra("filepath")
        }

        if (filepath == null) {
            Log.e(TAG, "No file given, exiting")
            finish()
            return
        }

        player.initialize(applicationContext.filesDir.path)
        player.addObserver(this)
        player.playFile(filepath)

        playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        // After hiding the interface with SYSTEM_UI_FLAG_HIDE_NAVIGATION the next tap only shows the UI without
        // calling dispatchTouchEvent. Use this to showControls even in this case.
        player.setOnSystemUiVisibilityChangeListener { vis ->
            if (vis == 0) {
                showControls()
            }
        }
    }

    override fun onDestroy() {
        player.destroy()
        super.onDestroy()
    }

    private fun getRealPathFromURI(contentUri: Uri): String {
        // http://stackoverflow.com/questions/3401579/#3414749
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = applicationContext.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } finally {
            if (cursor != null)
                cursor.close()
        }
    }

    private fun copyAssets() {
        val assetManager = applicationContext.assets
        val files = arrayOf("subfont.ttf")
        val configDir = applicationContext.filesDir.path
        for (filename in files) {
            var ins: InputStream? = null
            var out: OutputStream? = null
            try {
                ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
                val outFile = File("$configDir/$filename")
                // XXX: .available() officially returns an *estimated* number of bytes available
                // this is only accurate for generic streams, asset streams return the full file size
                if (outFile.length() == ins.available().toLong()) {
                    Log.w(TAG, "Skipping copy of asset file (exists same size): $filename")
                    continue
                }
                out = FileOutputStream(outFile)
                ins.copyTo(out)
                Log.w(TAG, "Copied asset file: $filename")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy asset file: $filename", e)
            } finally {
                ins?.close()
                out?.close()
            }
        }
    }

    override fun onPause() {
        player.onPause()
        super.onPause()
    }

    override fun onResume() {
        // Init controls to be hidden and view fullscreen
        initControls()

        player.onResume()
        super.onResume()
    }

    private fun showControls() {
        // remove all callbacks that were to be run for fading
        fadeHandler.removeCallbacks(fadeRunnable)

        // set the main controls as 75%, actual seek bar|buttons as 100%
        controls.alpha = 1f

        // Open, Sesame!
        controls.visibility = View.VISIBLE

        // add a new callback to hide the controls once again
        fadeHandler.postDelayed(fadeRunnable, CONTROLS_DISPLAY_TIMEOUT)
    }

    fun initControls() {
        /* Init controls to be hidden */
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        controls.visibility = View.GONE

        val flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility = flags
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        showControls()
        return super.dispatchKeyEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        showControls()
        return super.dispatchTouchEvent(ev)
    }

    fun playPause(view: View) = player.cyclePause()

    private fun showToast(msg: String) {
        toast.setText(msg)
        toast.show()
    }

    data class TrackData(val track_id: Int, val track_type: String)
    fun trackSwitchNotification(f: () -> TrackData) {
        val (track_id, track_type) = f()
        val track_prefix = when (track_type) {
            "audio" -> "Audio"
            "sub"   -> "Subs"
            "video" -> "Video"
            else    -> "Unknown"
        }

        if (track_id == -1) {
            showToast("$track_prefix Off")
            return
        }

        val track_name = player.tracks[track_type]?.firstOrNull{ it.mpvId == track_id }?.name ?: "???"
        showToast("$track_prefix $track_name")
    }

    fun cycleAudio() = trackSwitchNotification {
        player.cycleAudio(); TrackData(player.aid, "audio")
    }

    fun cycleSub() = trackSwitchNotification {
        player.cycleSub(); TrackData(player.sid, "sub")
    }

    private fun selectTrack(type: String, get: () -> Int, set: (Int) -> Unit) {
        val tracks = player.tracks[type]!!
        val selectedMpvId = get()
        val selectedIndex = tracks.indexOfFirst { it.mpvId == selectedMpvId }
        val wasPlayerPaused = player.paused ?: true // default to not changing state after switch

        player.paused = true

        with (AlertDialog.Builder(this)) {
            setSingleChoiceItems(tracks.map { it.name }.toTypedArray(), selectedIndex) { dialog, item ->
                set(tracks[item].mpvId)
                dialog.dismiss()
            }
            setOnDismissListener { if (!wasPlayerPaused) player.paused = false }
            create().show()
        }
    }

    fun pickAudio() = selectTrack("audio", { player.aid }, { player.aid = it })

    fun pickSub() = selectTrack("sub", { player.sid }, { player.sid = it })

    fun switchDecoder(view: View) {
        player.cycleHwdec()
        updateDecoderButton()
    }

    internal fun prettyTime(d: Int): String {
        val hours = d / 3600
        val minutes = d % 3600 / 60
        val seconds = d % 60
        if (hours == 0)
            return "%02d:%02d".format(minutes, seconds)
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun updatePlaybackPos(position: Int) {
        playbackPositionTxt.text = prettyTime(position)
        if (!userIsOperatingSeekbar)
            playbackSeekbar.progress = position
        updateDecoderButton()
    }

    fun updatePlaybackDuration(duration: Int) {
        playbackDurationTxt.text = prettyTime(duration)
        if (!userIsOperatingSeekbar)
            playbackSeekbar.max = duration
    }

    fun updatePlaybackStatus(paused: Boolean) {
        val r = if (paused) R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp
        playBtn.setImageResource(r)
    }

    fun updateDecoderButton() {
        cycleDecoderBtn.text = if (player.hwdecActive!!) "HW" else "SW"
    }

    fun eventPropertyUi(property: String) {
        when (property) {
            "track-list" -> player.loadTracks()
        }
    }

    fun eventPropertyUi(property: String, value: Boolean) {
        when (property) {
            "pause" -> updatePlaybackStatus(value)
        }
    }

    fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "time-pos" -> updatePlaybackPos(value.toInt())
            "duration" -> updatePlaybackDuration(value.toInt())
        }
    }

    fun eventPropertyUi(property: String, value: String) {
    }

    fun eventUi(eventId: Int) {
        when (eventId) {
            MPVLib.mpvEventId.MPV_EVENT_END_FILE -> finish()
        }
    }

    override fun eventProperty(property: String) {
        runOnUiThread { eventPropertyUi(property) }
    }

    override fun eventProperty(property: String, value: Boolean) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: Long) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: String) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun event(eventId: Int) {
        runOnUiThread { eventUi(eventId) }
    }

    companion object {
        private val TAG = "mpv"
        // how long should controls be displayed on screen
        private val CONTROLS_DISPLAY_TIMEOUT = 2000L
    }
}

internal class FadeOutControlsRunnable(private val activity: MPVActivity, private val controls: View) : Runnable {

    override fun run() {
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        controls.animate().alpha(0f).setDuration(500).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                activity.initControls()
            }
        })
    }
}
