package `is`.xyz.mpv.utils

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

fun animateView(view: View, enterOrExit: Boolean, duration: Long, delay: Long = 0) {
    when {
        view.isVisible && enterOrExit -> {
            view.animate().setListener(null).cancel()
            view.isVisible = true
            view.alpha = 1f
            return
        }
        !view.isVisible && !enterOrExit -> {
            view.animate().setListener(null).cancel()
            view.isGone = true
            view.alpha = 0f
            return
        }
    }
    view.animate().setListener(null).cancel()
    view.isVisible = true
    animateScaleAndAlpha(view, enterOrExit, duration, delay)
}

private fun animateScaleAndAlpha(view: View, enterOrExit: Boolean, duration: Long, delay: Long) {
    if (enterOrExit) {
        view.scaleX = .8f
        view.scaleY = .8f
        view.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setStartDelay(delay)
                .start()
    } else {
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate()
                .setInterpolator(FastOutSlowInInterpolator())
                .alpha(0f)
                .scaleX(.8f)
                .scaleY(.8f)
                .setDuration(duration).setStartDelay(delay)
                .start()
    }
}