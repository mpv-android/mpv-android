package `is`.xyz.mpv

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Watches for a connected external (secondary) display eligible for [android.app.Presentation]
 * (HDMI, USB-C DisplayPort-alt-mode, or an OS-managed wireless display) and shows/dismisses an
 * [ExternalDisplayPresentation] on it accordingly.
 */
class ExternalDisplayManager(
    private val context: Context,
    private val onStateChanged: (active: Boolean) -> Unit,
) {
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())

    private var presentation: ExternalDisplayPresentation? = null

    /** Settings-driven enable/disable flag; set from MPVActivity.readSettings(). */
    var enabled: Boolean = true
        set(value) {
            field = value
            reconcile()
        }

    val isActive: Boolean
        get() = presentation != null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = reconcile()
        override fun onDisplayRemoved(displayId: Int) = reconcile()
        override fun onDisplayChanged(displayId: Int) = reconcile()
    }

    fun start() {
        displayManager.registerDisplayListener(listener, handler)
        reconcile()
    }

    fun reconcile() {
        val target = if (enabled)
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()
        else
            null

        if (target == null || target.displayId != presentation?.display?.displayId) {
            // dismiss(), when a presentation actually exists, invokes the onDismissListener
            // below synchronously (we're always called from the main thread) - that already
            // clears `presentation` and notifies, so there's nothing more to do here.
            presentation?.dismiss()
        }

        if (target != null && presentation == null) {
            Log.v(TAG, "Showing presentation on display ${target.displayId} (${target.name})")
            presentation = ExternalDisplayPresentation(context, target).also {
                it.setOnDismissListener {
                    presentation = null
                    MPVSurfaceCoordinator.externalDisplayActive = false
                    onStateChanged(false)
                }
                it.show()
            }
            MPVSurfaceCoordinator.externalDisplayActive = true
            onStateChanged(true)
        }
    }

    fun release() {
        displayManager.unregisterDisplayListener(listener)
        presentation?.dismiss()
        presentation = null
        MPVSurfaceCoordinator.externalDisplayActive = false
    }

    companion object {
        private const val TAG = "mpv"
    }
}
