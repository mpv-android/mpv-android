package `is`.xyz.mpv

import kotlinx.android.synthetic.main.player.*

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.res.AssetManager
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MPVActivity : Activity(), EventObserver {

    internal var fadeHandler: Handler? = null
    internal var fadeRunnable: FadeOutControlsRunnable? = null

    internal var userIsOperatingSeekbar = false

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser)
                return
            mpv_view.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // Do copyAssets here and not in MainActivity because mpv can be launched from a file browser
        copyAssets()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.player)

        // Init controls to be hidden and view fullscreen
        initControls()

        // set up a callback handler and a runnable for fading the controls out
        fadeHandler = Handler()
        fadeRunnable = FadeOutControlsRunnable(this, controls)

        var filepath: String? = null

        val i = intent
        val action = i.action
        if (action != null && action == Intent.ACTION_VIEW) {
            // launched as viewer for a specific file
            val u = i.data
            if (u.scheme == "file")
                filepath = u.path
            else if (u.scheme == "content")
                filepath = getRealPathFromURI(u)
            else if (u.scheme == "http")
                filepath = u.toString()

            if (filepath == null) {
                Log.e(TAG, "unknown scheme: " + u.scheme)
            }
        } else {
            filepath = i.getStringExtra("filepath")
        }

        mpv_view.initialize(applicationContext.filesDir.path)
        mpv_view.addObserver(this)
        if (filepath != null)
            mpv_view.playFile(filepath)

        controls_seekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        // After hiding the interface with SYSTEM_UI_FLAG_HIDE_NAVIGATION the next tap only shows the UI without
        // calling dispatchTouchEvent. Use this to showControls even in this case.
        mpv_view.setOnSystemUiVisibilityChangeListener { vis ->
            if (vis == 0) {
                showControls()
            }
        }
    }

    override fun onDestroy() {
        mpv_view.destroy()
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
            val ins: InputStream
            val out: OutputStream
            try {
                ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
                val outFile = File(configDir + "/" + filename)
                // XXX: .available() officially returns an *estimated* number of bytes available
                // this is only accurate for generic streams, asset streams return the full file size
                if (outFile.length() == ins.available().toLong()) {
                    ins.close()
                    Log.w(TAG, "Skipping copy of asset file (exists same size): " + filename)
                    continue
                }
                out = FileOutputStream(outFile)

                ins.copyTo(out)
                ins.close()
                out.close()
                Log.w(TAG, "Copied asset file: " + filename)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy asset file: " + filename, e)
            }

        }
    }

    override fun onPause() {
        mpv_view.onPause()

        super.onPause()
    }

    override fun onResume() {
        // Init controls to be hidden and view fullscreen
        initControls()

        mpv_view.onResume()

        super.onResume()
    }

    private fun showControls() {
        // remove all callbacks that were to be run for fading
        fadeHandler!!.removeCallbacks(fadeRunnable)

        // set the main controls as 75%, actual seek bar|buttons as 100%
        controls.alpha = 1f

        // Open, Sesame!
        controls.visibility = View.VISIBLE

        // add a new callback to hide the controls once again
        fadeHandler!!.postDelayed(fadeRunnable, CONTROLS_DISPLAY_TIMEOUT.toLong())
    }

    fun initControls() {
        /* Init controls to be hidden */
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        controls.visibility = View.GONE

        val flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility = flags
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        showControls()
        return super.dispatchTouchEvent(ev)
    }

    fun playPause(view: View) {
        mpv_view.cyclePause()
    }

    fun cycleAudio(view: View) {
        mpv_view.cycleAudio()
    }

    fun cycleSub(view: View) {
        mpv_view.cycleSub()
    }

    fun switchDecoder(view: View) {
        mpv_view.cycleHwdec()
        updateDecoderButton()
    }

    internal fun prettyTime(d: Int): String {
        val hours = (d / 3600)
        val minutes = (d % 3600 / 60)
        val seconds = (d % 60)
        if (hours == 0)
            return String.format("%02d:%02d", minutes, seconds)
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    fun updatePlaybackPos(position: Int) {
        val positionView = findViewById(R.id.controls_position) as TextView
        positionView.text = prettyTime(position)
        if (!userIsOperatingSeekbar)
            controls_seekbar.progress = position
        updateDecoderButton()
    }

    fun updatePlaybackDuration(duration: Int) {
        val durationView = findViewById(R.id.controls_duration) as TextView
        durationView.text = prettyTime(duration)
        if (!userIsOperatingSeekbar)
            controls_seekbar.max = duration
    }

    fun updatePlaybackStatus(paused: Boolean) {
        val playBtn = findViewById(R.id.controls_play) as ImageButton
        if (paused)
            playBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp)
        else
            playBtn.setImageResource(R.drawable.ic_pause_black_24dp)
    }

    fun updateDecoderButton() {
        val switchDecoderBtn = findViewById(R.id.switchDecoder) as Button
        switchDecoderBtn.text = if (mpv_view.hwdecActive) "HW" else "SW"
    }

    fun eventPropertyUi(property: String) {
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
        private val CONTROLS_DISPLAY_TIMEOUT = 2000
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
