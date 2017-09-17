package `is`.xyz.mpv

import kotlinx.android.synthetic.main.player.*

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.media.AudioManager
import android.net.Uri
import android.preference.PreferenceManager.getDefaultSharedPreferences
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

class MPVActivity : Activity(), EventObserver, TouchGesturesObserver {
    lateinit internal var fadeHandler: Handler
    lateinit internal var fadeRunnable: FadeOutControlsRunnable

    internal var userIsOperatingSeekbar = false

    lateinit internal var toast: Toast
    lateinit private var gestures: TouchGestures
    lateinit private var audioManager: AudioManager

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

    private var statsEnabled = false
    private var statsOnlyFPS = false
    private var gesturesEnabled = true

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

    private var onload_commands = ArrayList<Array<String>>()

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

        syncSettings()

        val filepath: String?
        if (intent.action == Intent.ACTION_VIEW) {
            filepath = resolveUri(intent.data)
            parseIntentExtras(intent.extras)
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

        if (this.gesturesEnabled) {
            val dm = resources.displayMetrics
            gestures = TouchGestures(dm.widthPixels.toFloat(), dm.heightPixels.toFloat(), this)
            player.setOnTouchListener { _, e -> gestures.onTouchEvent(e) }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onDestroy() {
        player.destroy()
        super.onDestroy()
    }

    private fun copyAssets() {
        val assetManager = applicationContext.assets
        val files = arrayOf("subfont.ttf", "cacert.pem")
        val configDir = applicationContext.filesDir.path
        for (filename in files) {
            var ins: InputStream? = null
            var out: OutputStream? = null
            try {
                ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
                val outFile = File("$configDir/$filename")
                // Note that .available() officially returns an *estimated* number of bytes available
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

    private fun syncSettings() {
        // FIXME: settings should be in their own class completely
        val prefs = getDefaultSharedPreferences(this.applicationContext)

        this.statsEnabled = prefs.getBoolean("show_stats", false) or prefs.getBoolean("show_fps", false)
        this.statsOnlyFPS = prefs.getBoolean("show_fps", false)
        this.gesturesEnabled = prefs.getBoolean("touch_gestures", true)

        if (this.statsOnlyFPS)
            statsTextView.setTextColor((0xFF00FF00).toInt()) // green
    }

    override fun onResume() {
        // Init controls to be hidden and view fullscreen
        initControls()
        syncSettings()

        player.onResume()
        super.onResume()
    }

    private fun updateStats() {
        if (this.statsOnlyFPS) {
            statsTextView.text = "${player.estimated_vf_fps} FPS"
            return
        }

        val text = "File: ${player.filename}\n\n" +
                "Video: ${player.video_codec} hwdec: ${player.hwdecActive}\n" +
                "\tA-V: ${player.avsync}\n" +
                "\tDropped: ${player.drop_frame_count} VO: ${player.vo_drop_frame_count}\n" +
                "\tFPS: ${player.fps} (specified) ${player.estimated_vf_fps} (estimated)\n" +
                "\tResolution: ${player.video_w}x${player.video_h}\n\n" +
                "Audio: ${player.audio_codec}\n" +
                "\tSample rate: ${player.audio_samplerate} Hz\n" +
                "\tChannels: ${player.audio_channels}"
        statsTextView.text = text
    }

    private fun showControls() {
        // remove all callbacks that were to be run for fading
        fadeHandler.removeCallbacks(fadeRunnable)

        // set the main controls as 75%, actual seek bar|buttons as 100%
        controls.alpha = 1f

        // Open, Sesame!
        controls.visibility = View.VISIBLE

        if (this.statsEnabled) {
            updateStats()
            statsTextView.visibility = View.VISIBLE
        }

        window.decorView.systemUiVisibility = 0

        // add a new callback to hide the controls once again
        fadeHandler.postDelayed(fadeRunnable, CONTROLS_DISPLAY_TIMEOUT)
    }

    fun initControls() {
        /* Init controls to be hidden */
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        controls.visibility = View.GONE
        statsTextView.visibility = View.GONE

        val flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.decorView.systemUiVisibility = flags
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        showControls()
        return super.dispatchKeyEvent(ev)
    }

    var mightWantToShowControls = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN)
            mightWantToShowControls = true
        if (ev.action == MotionEvent.ACTION_UP && mightWantToShowControls)
            showControls()
        return super.dispatchTouchEvent(ev)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_CAPTIONS -> cycleSub()
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> player.cyclePause()
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> cycleAudio()
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> seekRelative(BUTTON_SEEK_RANGE)
            KeyEvent.KEYCODE_MEDIA_PAUSE -> player.paused = true
            KeyEvent.KEYCODE_MEDIA_PLAY -> player.paused = false
            KeyEvent.KEYCODE_MEDIA_REWIND -> seekRelative(-BUTTON_SEEK_RANGE)
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    fun playPause(view: View) = player.cyclePause()

    private fun showToast(msg: String) {
        toast.setText(msg)
        toast.show()
    }

    private fun seekRelative(offset: Int) {
        MPVLib.command(arrayOf("seek", offset.toString(), "relative"))
    }

    internal fun resolveUri(data: Uri): String? {
        val filepath = when (data.scheme) {
            "file" -> data.path
            "content" -> openContentFd(data)
            "http", "https" -> data.toString()
            else -> null
        }

        if (filepath == null)
            Log.e(TAG, "unknown scheme: ${data.scheme}")
        return filepath
    }

    private fun openContentFd(uri: Uri): String? {
        val resolver = applicationContext.getContentResolver()
        try {
            val fd = resolver.openFileDescriptor(uri, "r")
            return "fdclose://${fd.detachFd()}"
        } catch(e: Exception) {
            Log.e(TAG, "Failed to open content fd: ${e.toString()}")
            return null
        }
    }

    inline private fun <reified T> cast(any: Any?, fallback: T) : T = any as? T ?: fallback

    private fun parseIntentExtras(extras: Bundle?) {
        onload_commands.clear()
        if (extras == null)
            return

        // API reference: http://mx.j2inter.com/api (partially implemented)
        if (extras.getByte("decode_mode") == 2.toByte())
            onload_commands.add(arrayOf("set", "file-local-options/hwdec", "no"))
        if (extras.containsKey("subs")) {
            val list = extras.getParcelableArray("subs") as Array<Uri>
            val list2 = if (!extras.containsKey("subs.enable"))
                emptyArray()
            else
                extras.getParcelableArray("subs.enable") as Array<Uri>
            for (suburi in list) {
                val subfile = resolveUri(suburi)
                if (subfile == null)
                    continue
                val flag = if (list2.filter({ it.compareTo(suburi) == 0 }).any()) "select" else "auto"

                Log.v(TAG, "Adding subtitles from intent extras: $subfile")
                onload_commands.add(arrayOf("sub-add", subfile, flag))
            }
        }
        if (extras.getInt("position", 0) > 0) {
            val pos = extras.getInt("position", 0) / 1000f
            onload_commands.add(arrayOf("set", "start", pos.toString()))
        }
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
                val track_id = tracks[item].mpvId

                set(track_id)
                dialog.dismiss()
                trackSwitchNotification { TrackData(track_id, type) }
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
        // explicitly not in ui thread
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_START_FILE) {
            for (c in onload_commands)
                MPVLib.command(c)
        }
        runOnUiThread { eventUi(eventId) }
    }

    private var initialSeek = 0
    private var initialBright = 0f
    private var initialVolume = 0
    private var maxVolume = 0

    override fun onPropertyChange(p: PropertyChange, diff: Float) {
        when (p) {
            PropertyChange.Init -> {
                mightWantToShowControls = false

                initialSeek = player.timePos ?: -1
                initialBright = 0.5f // TODO: what about default brightness?
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                gestureTextView.visibility = View.VISIBLE
                gestureTextView.text = ""
            }
            PropertyChange.Seek -> {
                // disable seeking on livestreams and when timePos is not available
                if (player.duration == null || initialSeek < 0)
                    return
                val newPos = Math.min(Math.max(0, initialSeek + diff.toInt()), player.duration!!)
                val newDiff = newPos - initialSeek
                // seek faster than assigning to timePos but less precise
                MPVLib.command(arrayOf("seek", newPos.toString(), "absolute", "keyframes"))
                updatePlaybackPos(newPos)

                val diffText = (if (newDiff >= 0) "+" else "-") + prettyTime(Math.abs(newDiff.toInt()))
                gestureTextView.text = "${prettyTime(newPos)}\n[$diffText]"
            }
            PropertyChange.Volume -> {
                val newVolume = Math.min(Math.max(0, initialVolume + (diff * maxVolume).toInt()), maxVolume)
                val newVolumePercent = 100 * newVolume / maxVolume
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

                gestureTextView.text = "V: $newVolumePercent%"
            }
            PropertyChange.Bright -> {
                val lp = window.attributes
                val newBright = Math.min(Math.max(0f, initialBright + diff), 1f)
                lp.screenBrightness = newBright
                window.attributes = lp

                gestureTextView.text = "B: ${Math.round(newBright * 100)}%"
            }
            PropertyChange.Finalize -> gestureTextView.visibility = View.GONE
        }
    }

    companion object {
        private val TAG = "mpv"
        // how long should controls be displayed on screen
        private val CONTROLS_DISPLAY_TIMEOUT = 2000L
        // how far to seek backward/forward with (currently) TV remote buttons
        private val BUTTON_SEEK_RANGE = 10
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
