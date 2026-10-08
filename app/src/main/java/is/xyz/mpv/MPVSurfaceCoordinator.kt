package `is`.xyz.mpv

import android.util.Log
import android.view.SurfaceHolder

/**
 * Arbitrates which [SurfaceHolder] libmpv's video output is bound to.
 *
 * libmpv exposes exactly one render target ("wid") at a time (see render.cpp: a single static
 * `surface` global). mpv-android can have two live surfaces simultaneously though: the phone's
 * [MPVView] and, once an external display is connected, an [ExternalDisplayPresentation]'s
 * [android.view.SurfaceView]. This object is the *only* thing allowed to call
 * [MPVLib.attachSurface]/[MPVLib.detachSurface] or touch the `vo`/`force-window` properties, so
 * exactly one surface owns `wid` at any time and a switch always goes through a clean
 * detach-then-attach, matching the sequence already proven correct for the single-surface case.
 */
object MPVSurfaceCoordinator {
    private const val TAG = "mpv"

    private var voInUse: String = "gpu"
    private var pendingFilePath: String? = null

    private var phoneSurface: SurfaceHolder? = null
    private var presentationSurface: SurfaceHolder? = null

    /** Set by [ExternalDisplayManager] when a Presentation is being shown/dismissed. */
    var externalDisplayActive: Boolean = false
        set(value) {
            field = value
            reconcile()
        }

    private var currentTarget: SurfaceHolder? = null

    /**
     * Clears all state. Since this is a process-wide singleton, but libmpv's core and the phone's
     * MPVView are recreated per-MPVActivity-instance (e.g. finish one video, then open another
     * via "Open with" - same process, fresh Activity), stale [SurfaceHolder] references from a
     * previous instance could otherwise linger. Call this once at the very start of
     * [BaseMPVView.initialize], before [MPVLib.create] - i.e. before any MPVLib call would be
     * valid, which is why this only touches plain fields and never reaches reconcile()'s MPVLib
     * calls (both surfaces are already null by the time externalDisplayActive is reset).
     */
    fun reset() {
        phoneSurface = null
        presentationSurface = null
        currentTarget = null
        pendingFilePath = null
        voInUse = "gpu"
        externalDisplayActive = false
    }

    /**
     * Sets the VO to use. It is automatically disabled/enabled as the active surface changes.
     */
    fun setVo(vo: String) {
        voInUse = vo
        MPVLib.setOptionString("vo", vo)
    }

    /**
     * Set the first file to be played once a surface is ready.
     */
    fun setPendingFilePath(filePath: String) {
        pendingFilePath = filePath
    }

    fun phoneSurfaceCreated(holder: SurfaceHolder) {
        phoneSurface = holder
        reconcile()
    }

    fun phoneSurfaceChanged(holder: SurfaceHolder, width: Int, height: Int) {
        reportSizeIfCurrent(holder, width, height)
    }

    fun phoneSurfaceDestroyed(holder: SurfaceHolder) {
        if (phoneSurface == holder)
            phoneSurface = null
        reconcile()
    }

    fun presentationSurfaceCreated(holder: SurfaceHolder) {
        presentationSurface = holder
        reconcile()
    }

    fun presentationSurfaceChanged(holder: SurfaceHolder, width: Int, height: Int) {
        reportSizeIfCurrent(holder, width, height)
    }

    fun presentationSurfaceDestroyed(holder: SurfaceHolder) {
        if (presentationSurface == holder)
            presentationSurface = null
        reconcile()
    }

    private fun reportSizeIfCurrent(holder: SurfaceHolder, width: Int, height: Int) {
        if (currentTarget == holder)
            MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    private fun reconcile() {
        val desiredTarget = if (externalDisplayActive && presentationSurface != null)
            presentationSurface
        else
            phoneSurface

        if (desiredTarget == currentTarget)
            return

        if (currentTarget != null) {
            Log.w(TAG, "detaching surface")
            // Note that before calling detachSurface() we need to be sure that libmpv
            // is done using the surface.
            // FIXME: There could be a race condition here, because I don't think
            // setting a property will wait for VO deinit.
            MPVLib.setPropertyString("vo", "null")
            MPVLib.setOptionString("force-window", "no")
            MPVLib.detachSurface()
        }

        currentTarget = desiredTarget

        if (desiredTarget != null) {
            Log.w(TAG, "attaching surface")
            MPVLib.attachSurface(desiredTarget.surface)
            // This forces mpv to render subs/osd/whatever into our surface even if it would
            // ordinarily not
            MPVLib.setOptionString("force-window", "yes")

            val filePath = pendingFilePath
            if (filePath != null) {
                MPVLib.command(arrayOf("loadfile", filePath))
                pendingFilePath = null
            } else {
                // We disable video output when the previous target disappears, enable it back
                MPVLib.setPropertyString("vo", voInUse)
            }
        }
    }
}
