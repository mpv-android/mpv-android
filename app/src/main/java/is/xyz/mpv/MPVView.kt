package `is`.xyz.mpv

import android.content.Context
import android.media.AudioManager
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent

import java.util.HashMap
import java.util.Objects

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

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
        val am = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        Log.v(TAG, "Device reports optimal frames per buffer $framesPerBuffer sample rate $sampleRate")
        MPVLib.setOptionString("ao", "opensles:frames-per-buffer=$framesPerBuffer:sample-rate=$sampleRate")
    }

    fun playFile(filePath: String) {
        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 3.0 or later backwards-compatible versions.
        Log.w(TAG + " [tid: " + Thread.currentThread().id + "]", "Setting EGLContextFactory")
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
        pause()
        super.onPause()
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        // At this point Renderer is already dead so it won't call step/draw, as such it's safe to free mpv resources
        MPVLib.clearObservers()
        MPVLib.destroy()
    }

    fun observeProperties() {
        val p = HashMap<String, MPVLib.mpvFormat>()
        p.put("time-pos", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        p.put("duration", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        p.put("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        for (property in p.entries)
            MPVLib.observeProperty(property.key, property.value.value)
    }

    fun addObserver(o: EventObserver) {
        MPVLib.addObserver(o)
    }

    val isPaused: Boolean
        get() = MPVLib.getPropertyBoolean("pause")

    val duration: Int
        get() = MPVLib.getPropertyInt("duration")

    var timePos: Int
        get() = MPVLib.getPropertyInt("time-pos")
        set(progress) = MPVLib.setPropertyInt("time-pos", progress)

    fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
    }

    fun cyclePause() {
        MPVLib.command(arrayOf("cycle", "pause"))
    }

    fun cycleAudio() {
        MPVLib.command(arrayOf("cycle", "audio"))
    }

    fun cycleSub() {
        MPVLib.command(arrayOf("cycle", "sub"))
    }

    fun cycleHwdec() {
        val next = if (isHwdecActive) "no" else "mediacodec"
        MPVLib.setPropertyString("hwdec", next)
    }

    val isHwdecActive: Boolean
        get() = MPVLib.getPropertyBoolean("hwdec-active")

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
                MPVLib.command(arrayOf<String>("loadfile", filePath as String))
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
