package `is`.xyz.mpv

import android.content.Context
import android.media.AudioManager
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import `is`.xyz.mpv.MPVLib.mpvFormat.*
import kotlin.reflect.KProperty

internal class MPVView(context: Context, attrs: AttributeSet) : GLSurfaceView(context, attrs) {

    fun initialize(configDir: String) {
        MPVLib.create()
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        MPVLib.init()
        initOptions()
        observeProperties()
    }

    fun initOptions() {
        MPVLib.setOptionString("hwdec", "mediacodec")
        MPVLib.setOptionString("vo", "opengl-cb")
        // set optimal buffer size and sample rate for opensles, to get better audio playback
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        Log.v(TAG, "Device reports optimal frames per buffer $framesPerBuffer sample rate $sampleRate")
        MPVLib.setOptionString("ao", "opensles:frames-per-buffer=$framesPerBuffer:sample-rate=$sampleRate")
    }

    fun playFile(filePath: String) {
        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 3.0 or later backwards-compatible versions.
        setEGLConfigChooser(8, 8, 8, 0, 16, 0)
        setEGLContextClientVersion(2)
        var renderer = Renderer()
        renderer.setFilePath(filePath)
        setRenderer(renderer)
    }

    override fun onPause() {
        queueEvent {
            MPVLib.destroyGL()
        }
        paused = true
        super.onPause()
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        // At this point Renderer is already dead so it won't call step/draw, as such it's safe to free mpv resources
        MPVLib.clearObservers()
        MPVLib.destroy()
    }

    fun observeProperties() {
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
        get() = MPVLib.getPropertyBoolean("hwdec-active")

    class TrackDelegate {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = MPVLib.getPropertyString(property.name)
            return if (v == "no") -1 else v.toInt()
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
    fun cycleHwdec() = MPVLib.setPropertyString("hwdec", if (hwdecActive!!) "no" else "mediacodec")

    fun seek(amount: Int) = MPVLib.command(arrayOf("seek", "$amount"))

    private class Renderer : GLSurfaceView.Renderer {
        private var filePath: String? = null

        override fun onDrawFrame(gl: GL10) {
            MPVLib.step()
            MPVLib.draw()
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            MPVLib.resize(width, height)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            Log.w(TAG, "Creating libmpv GL surface")
            MPVLib.initGL()
            if (filePath != null) {
                MPVLib.command(arrayOf("loadfile", filePath as String))
                filePath = null
            } else {
                // Get here when user goes to home screen and then returns to the app
                // mpv disables video output when opengl context is destroyed, enable it back
                MPVLib.setPropertyInt("vid", 1)
            }
        }

        fun setFilePath(file_path: String) {
            filePath = file_path
        }
    }

    companion object {
        private val TAG = "mpv"
    }
}
