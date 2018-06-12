package `is`.xyz.mpv

import android.content.Context
import android.media.AudioManager
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.util.AttributeSet
import android.util.Log
import android.view.WindowManager

import `is`.xyz.mpv.MPVLib.mpvFormat.*
import android.annotation.SuppressLint
import android.os.Build
import android.preference.PreferenceManager
import kotlin.reflect.KProperty

internal class MPVView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback, EventObserver {

    // Whether or not we have a surface active, this is used by playFile and flip-flopped by
    // surfaceCreated and surfaceDestroyed
    private var haveSurface = false

    // File path to play next time we get a surface
    private var delayedFileLoad = ""

    private var onloadCommands: ArrayList<Array<String>> = arrayListOf()

    fun initialize(configDir: String) {
        holder.addCallback(this)
        MPVLib.create(this.context)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        MPVLib.init()
        initOptions()
        observeProperties()
        addObserver(this)
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

        // ao: set optimal buffer size and sample rate for opensles, to get better audio playback
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        Log.v(TAG, "Device reports optimal frames per buffer $framesPerBuffer sample rate $sampleRate")

        MPVLib.setOptionString("opensles-frames-per-buffer", framesPerBuffer)
        MPVLib.setOptionString("audio-samplerate", sampleRate)

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

        val vidsync = sharedPreferences.getString("video_sync", resources.getString(R.string.pref_video_sync_default))
        MPVLib.setOptionString("video-sync", vidsync)

        if (sharedPreferences.getBoolean("video_interpolation", false)) {
            if (!vidsync.startsWith("display-"))
                Log.e(TAG, "Interpolation enabled but video-sync not set to a 'display' mode, this won't work!")
            MPVLib.setOptionString("interpolation", "yes")
        }

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
        // Limit demuxer cache to 32 MiB, the default is too high for mobile devices
        MPVLib.setOptionString("demuxer-max-bytes", "${32 * 1024 * 1024}")
        MPVLib.setOptionString("demuxer-max-back-bytes", "${32 * 1024 * 1024}")
    }

    fun playFile(filePath: String) {
        // We can get here when we already have a surface or when we don't have one yet
        // - if we have a surface, play the file immediately
        // - if we don't have a surface, queue loadfile request for the next surfaceCreated call
        synchronized(haveSurface) {
            delayedFileLoad = filePath
            if (haveSurface)
                playFileInternal()
        }
    }

    fun setOnloadCommands(commands: ArrayList<Array<String>>) {
        synchronized(onloadCommands, {
            onloadCommands = commands
        })
    }

    private fun playFileInternal() {
        MPVLib.command(arrayOf("loadfile", delayedFileLoad))
        delayedFileLoad = ""
    }

    fun onPause() {
        MPVLib.setPropertyString("vid", "no")
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

    private fun observeProperties() {
        data class Property(val name: String, val format: Int)
        val p = arrayOf(
                Property("time-pos", MPV_FORMAT_INT64),
                Property("duration", MPV_FORMAT_INT64),
                Property("pause", MPV_FORMAT_FLAG),
                Property("track-list", MPV_FORMAT_NONE)
        )

        for ((name, format) in p)
            MPVLib.observeProperty(name, format)
    }

    fun addObserver(o: EventObserver) {
        MPVLib.addObserver(o)
    }
    fun removeObserver(o: EventObserver) {
        MPVLib.removeObserver(o)
    }

    data class Track(val mpvId: Int, val name: String)
    var tracks = mapOf<String, MutableList<Track>>(
            "audio" to arrayListOf(),
            "video" to arrayListOf(),
            "sub" to arrayListOf())

    fun loadTracks() {
        for (type in tracks.keys) {
            tracks[type]!!.clear()
            // pseudo-track to allow disabling audio/subs
            tracks[type]!!.add(Track(-1, "None"))
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
            tracks[type]!!.add(track)
        }
    }

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

    class TrackDelegate {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = MPVLib.getPropertyString(property.name)
            // we can get null here for "no" or other invalid value
            return v.toIntOrNull() ?: -1
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

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        synchronized(haveSurface) {
            Log.w(TAG, "Creating libmpv Surface")
            MPVLib.attachSurface(holder.surface)
            if (delayedFileLoad.isNotEmpty()) {
                playFileInternal()
            } else {
                // Get here when user goes to home screen and then returns to the app
                // mpv disables video output when opengl context is destroyed, enable it back
                MPVLib.setPropertyInt("vid", 1)
            }
            haveSurface = true
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(haveSurface) {
            haveSurface = false
            MPVLib.detachSurface()
        }
    }

    override fun eventProperty(property: String) {}
    override fun eventProperty(property: String, value: Long) {}
    override fun eventProperty(property: String, value: Boolean) {}
    override fun eventProperty(property: String, value: String) {}

    override fun event(eventId: Int) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_START_FILE) {
            synchronized(onloadCommands, {
                for (cmd in onloadCommands) {
                    MPVLib.command(cmd)
                }
                onloadCommands.clear()
            })
        }
    }

    companion object {
        private const val TAG = "mpv"
    }
}
