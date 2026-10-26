package `is`.xyz.mpv

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.SurfaceView
import android.view.ViewGroup

/**
 * Minimal window shown on an external (secondary) display: just a full-bleed [SurfaceView] that
 * registers itself with [MPVSurfaceCoordinator], the same way the phone's [MPVView] does.
 */
class ExternalDisplayPresentation(
    context: Context,
    display: Display,
) : Presentation(context, display) {
    private val surfaceCallback = MPVSurfaceCallback.forPresentation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surfaceView = SurfaceView(context)
        setContentView(surfaceView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        surfaceView.holder.addCallback(surfaceCallback)
    }
}
