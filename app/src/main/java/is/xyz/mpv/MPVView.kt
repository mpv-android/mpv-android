package `is`.xyz.mpv

import android.content.Context
import android.media.AudioTrack
import android.media.AudioManager
import android.util.AttributeSet
import android.util.Log

import `is`.xyz.mpv.MPVLib.mpvFormat.*
import android.annotation.SuppressLint
import android.os.Build
import android.preference.PreferenceManager
import android.view.*
import kotlin.math.abs
import kotlin.reflect.KProperty

internal class MPVView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    fun initialize(configDir: String) {
        holder.addCallback(this)
        MPVLib.create(this.context)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        MPVLib.init()
        initOptions()
        observeProperties()
    }


    @SuppressLint("NewApi")
    private fun initOptions() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context)

        // initial options
        data class Property(val preference_name: String, val mpv_option: String)

        // hwdec
        val hwdec = if (sharedPreferences.getBoolean("hardware_decoding", true))
            "mediacodec-copy"
        else
            "no"

        // vo: set display fps as reported by android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val disp = wm.defaultDisplay
            val refreshRate = disp.mode.refreshRate

            Log.v(TAG, "Display ${disp.displayId} reports FPS of $refreshRate")
            MPVLib.setOptionString("display-fps", refreshRate.toString())
        } else {
            Log.v(TAG, "Android version too old, disabling refresh rate functionality " +
                       "(${Build.VERSION.SDK_INT} < ${Build.VERSION_CODES.M})")
        }

        // ao: set optimal sample rate for opensles, to get better audio playback
        val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        Log.v(TAG, "Device reports optimal sample rate $sampleRate")

        // TODO: better be optional as it may not be ideal if the user switches audio device during playback.
        MPVLib.setOptionString("audio-samplerate", sampleRate.toString())

        // set non-complex options

        val opts = arrayOf(
                Property("default_audio_language", "alang"),
                Property("default_subtitle_language", "slang"),

                // vo-related
                Property("video_scale", "scale"),
                Property("video_scale_param1", "scale-param1"),
                Property("video_scale_param2", "scale-param2"),

                Property("video_downscale", "dscale"),
                Property("video_downscale_param1", "dscale-param1"),
                Property("video_downscale_param2", "dscale-param2"),

                Property("video_tscale", "tscale"),
                Property("video_tscale_param1", "tscale-param1"),
                Property("video_tscale_param2", "tscale-param2")
        )

        for ((preference_name, mpv_option) in opts) {
            val preference = sharedPreferences.getString(preference_name, "")
            if (!preference.isNullOrBlank())
                MPVLib.setOptionString(mpv_option, preference)
        }

        val debandMode = sharedPreferences.getString("video_debanding", "")
        if (debandMode == "gradfun") {
            // lower the default radius (16) to improve performance
            MPVLib.setOptionString("vf", "gradfun=radius=12")
        } else if (debandMode == "gpu") {
            MPVLib.setOptionString("deband", "yes")
        }

        val vidsync = sharedPreferences.getString("video_sync", resources.getString(R.string.pref_video_interpolation_sync_default))
        MPVLib.setOptionString("video-sync", vidsync)

        if (sharedPreferences.getBoolean("video_interpolation", false))
            MPVLib.setOptionString("interpolation", "yes")

        if (sharedPreferences.getBoolean("gpudebug", false))
            MPVLib.setOptionString("gpu-debug", "yes")

        if (sharedPreferences.getBoolean("video_fastdecode", false)) {
            MPVLib.setOptionString("vd-lavc-fast", "yes")
            MPVLib.setOptionString("vd-lavc-skiploopfilter", "nonkey")
        }

        // set options

        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("hwdec", hwdec)
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9")
        MPVLib.setOptionString("ao", "opensles")
        MPVLib.setOptionString("tls-verify", "yes")
        MPVLib.setOptionString("tls-ca-file", "${this.context.filesDir.path}/cacert.pem")
        MPVLib.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache to 32 MiB, the default is too high for mobile devices
        MPVLib.setOptionString("demuxer-max-bytes", "${32 * 1024 * 1024}")
        MPVLib.setOptionString("demuxer-max-back-bytes", "${32 * 1024 * 1024}")
        MPVLib.setOptionString("save-position-on-quit", "no") // done manually by MPVActivity
    }

    fun playFile(filePath: String) {
        this.filePath = filePath
    }

    fun onPause(actuallyPause: Boolean) {
        MPVLib.setPropertyString("vid", "no")
        if (actuallyPause)
            paused = true
    }
    
    fun onResume() {
        // Interruptions can happen without the surface being destroyed,
        // so we need to cover this case too and reenable video output
        if (holder.surface != null && holder.surface.isValid) {
            Log.w(TAG, "Valid non-null surface received in onResume: '${holder.surface}'")
            MPVLib.setPropertyInt("vid", 1)
        }
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        // Disable surface callbacks to avoid using unintialized mpv state
        holder.removeCallback(this)

        MPVLib.destroy()
    }

    fun onPointerEvent(event: MotionEvent): Boolean {
        assert (event.isFromSource(InputDevice.SOURCE_CLASS_POINTER))
        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val h = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val v = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (abs(h) > 0)
                MPVLib.command(arrayOf("keypress", if (h < 0) "WHEEL_LEFT" else "WHEEL_RIGHT"))
            if (abs(v) > 0)
                MPVLib.command(arrayOf("keypress", if (v < 0) "WHEEL_DOWN" else "WHEEL_UP"))
            return true
        }
        return false
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_MULTIPLE)
            return false
        if (KeyEvent.isModifierKey(event.keyCode))
            return false

        var mapped = KeyMapping.map.get(event.keyCode)
        if (mapped == null) {
            // Fallback to produced glyph
            if (!event.isPrintingKey) {
                if (event.repeatCount == 0)
                    Log.d(TAG, "Unmapped non-printable key ${event.keyCode}")
                return false
            }

            val ch = event.unicodeChar
            if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0)
                return false // dead key
            mapped = ch.toChar().toString()
        }

        if (event.repeatCount > 0)
            return true // eat event but ignore it, mpv has its own key repeat

        val mod: MutableList<String> = mutableListOf()
        event.isShiftPressed && mod.add("shift")
        event.isCtrlPressed && mod.add("ctrl")
        event.isAltPressed && mod.add("alt")
        event.isMetaPressed && mod.add("meta")

        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        mod.add(mapped)
        MPVLib.command(arrayOf(action, mod.joinToString("+")))

        return true
    }

    private fun observeProperties() {
        data class Property(val name: String, val format: Int)
        val p = arrayOf(
                Property("time-pos", MPV_FORMAT_INT64),
                Property("duration", MPV_FORMAT_INT64),
                Property("pause", MPV_FORMAT_FLAG),
                Property("track-list", MPV_FORMAT_NONE),
                Property("video-params", MPV_FORMAT_NONE),
                Property("playlist-pos", MPV_FORMAT_NONE),
                Property("playlist-count", MPV_FORMAT_NONE)
        )

        for ((name, format) in p)
            MPVLib.observeProperty(name, format)
    }

    fun addObserver(o: MPVLib.EventObserver) {
        MPVLib.addObserver(o)
    }
    fun removeObserver(o: MPVLib.EventObserver) {
        MPVLib.removeObserver(o)
    }

    data class Track(val mpvId: Int, val name: String)
    var tracks = mapOf<String, MutableList<Track>>(
            "audio" to arrayListOf(),
            "video" to arrayListOf(),
            "sub" to arrayListOf())

    fun loadTracks() {
        for (type in tracks.keys) {
            tracks.getValue(type).clear()
            // pseudo-track to allow disabling audio/subs
            tracks.getValue(type).add(Track(-1, "None"))
        }
        val count = MPVLib.getPropertyInt("track-list/count")!!
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type")!!
            if (!tracks.containsKey(type)) {
                Log.w(TAG, "Got unknown track type: $type")
                continue
            }
            val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "unk"
            val mpvId = MPVLib.getPropertyInt("track-list/$i/id")!!
            val track = Track(
                    mpvId=mpvId,
                    name="#$mpvId: $lang"
                    )
            tracks.getValue(type).add(track)
        }
    }

    data class PlaylistFile(val index: Int, val name: String)

    fun loadPlaylist(): MutableList<PlaylistFile> {
        val playlist: MutableList<PlaylistFile> = mutableListOf()
        val count = MPVLib.getPropertyInt("playlist-count")!!
        for (i in 0 until count) {
            val filename = MPVLib.getPropertyString("playlist/$i/filename")!!
                    .replaceBeforeLast('/', "").trimStart('/')
            val title = MPVLib.getPropertyString("playlist/$i/title")
            playlist.add(PlaylistFile(
                    index=i,
                    name=title ?: filename
            ))
        }
        return playlist
    }

    private var filePath: String? = null

    // Property getters/setters

    var paused: Boolean?
        get() = MPVLib.getPropertyBoolean("pause")
        set(paused) = MPVLib.setPropertyBoolean("pause", paused)

    val duration: Int?
        get() = MPVLib.getPropertyInt("duration")

    var timePos: Int?
        get() = MPVLib.getPropertyInt("time-pos")
        set(progress) = MPVLib.setPropertyInt("time-pos", progress)

    val hwdecActive: Boolean?
        get() = MPVLib.getPropertyString("hwdec-current") != "no"

    var playbackSpeed: Double?
        get() = MPVLib.getPropertyDouble("speed")
        set(speed) = MPVLib.setPropertyDouble("speed", speed)

    val filename: String?
        get() = MPVLib.getPropertyString("filename")

    val avsync: String?
        get() = MPVLib.getPropertyString("avsync")

    val decoderFrameDropCount: Int?
        get() = MPVLib.getPropertyInt("decoder-frame-drop-count")

    val frameDropCount: Int?
        get() = MPVLib.getPropertyInt("frame-drop-count")

    val fps: String?
        get() = MPVLib.getPropertyString("fps")

    val estimatedVfFps: String?
        get() = MPVLib.getPropertyString("estimated-vf-fps")

    val videoW: Int?
        get() = MPVLib.getPropertyInt("video-params/w")

    val videoH: Int?
        get() = MPVLib.getPropertyInt("video-params/h")

    val videoCodec: String?
        get() = MPVLib.getPropertyString("video-codec")

    val audioCodec: String?
        get() = MPVLib.getPropertyString("audio-codec")

    val audioSampleRate: Int?
        get() = MPVLib.getPropertyInt("audio-params/samplerate")

    val audioChannels: Int?
        get() = MPVLib.getPropertyInt("audio-params/channel-count")

    val vid: Int
        get() = MPVLib.getPropertyString("vid")?.toIntOrNull() ?: -1

    class TrackDelegate {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = MPVLib.getPropertyString(property.name)
            // we can get null here for "no" or other invalid value
            return v?.toIntOrNull() ?: -1
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            if (value == -1)
                MPVLib.setPropertyString(property.name, "no")
            else
                MPVLib.setPropertyInt(property.name, value)
        }
    }

    var sid: Int by TrackDelegate()
    var aid: Int by TrackDelegate()

    // Commands

    fun cyclePause() = MPVLib.command(arrayOf("cycle", "pause"))
    fun cycleAudio() = MPVLib.command(arrayOf("cycle", "audio"))
    fun cycleSub() = MPVLib.command(arrayOf("cycle", "sub"))
    fun cycleHwdec() = MPVLib.setPropertyString("hwdec", if (hwdecActive!!) "no" else "mediacodec-copy")

    fun cycleSpeed() {
        val speeds = arrayOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0)
        playbackSpeed = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    // Surface callbacks

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.w(TAG, "Creating libmpv Surface")
        MPVLib.attachSurface(holder.surface)
        if (filePath != null) {
            MPVLib.command(arrayOf("loadfile", filePath as String))
            filePath = null
        } else {
            // Get here when user goes to home screen and then returns to the app
            // mpv disables video output when opengl context is destroyed, enable it back
            MPVLib.setPropertyInt("vid", 1)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        MPVLib.detachSurface()
    }

    companion object {
        private const val TAG = "mpv"
    }
}
