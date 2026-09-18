package `is`.xyz.mpv

import android.view.SurfaceHolder

/**
 * Generic [SurfaceHolder.Callback] that forwards events to [MPVSurfaceCoordinator], which decides
 * whether this particular surface actually gets to be libmpv's render target. Used both by the
 * phone's [MPVView] and by [ExternalDisplayPresentation]'s SurfaceView.
 */
class MPVSurfaceCallback(
    private val onCreated: (SurfaceHolder) -> Unit,
    private val onChanged: (SurfaceHolder, Int, Int) -> Unit,
    private val onDestroyed: (SurfaceHolder) -> Unit,
) : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) = onCreated(holder)

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
        onChanged(holder, width, height)

    override fun surfaceDestroyed(holder: SurfaceHolder) = onDestroyed(holder)

    companion object {
        fun forPhone() = MPVSurfaceCallback(
            onCreated = MPVSurfaceCoordinator::phoneSurfaceCreated,
            onChanged = MPVSurfaceCoordinator::phoneSurfaceChanged,
            onDestroyed = MPVSurfaceCoordinator::phoneSurfaceDestroyed,
        )

        fun forPresentation() = MPVSurfaceCallback(
            onCreated = MPVSurfaceCoordinator::presentationSurfaceCreated,
            onChanged = MPVSurfaceCoordinator::presentationSurfaceChanged,
            onDestroyed = MPVSurfaceCoordinator::presentationSurfaceDestroyed,
        )
    }
}
