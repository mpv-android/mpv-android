package `is`.xyz.mpv

import android.graphics.SurfaceTexture

class NativeOnFrameAvailableListener: SurfaceTexture.OnFrameAvailableListener {
    var nativePtr = 0L

    override fun onFrameAvailable(texture: SurfaceTexture?) {
        nativeOnFrameAvailable(nativePtr)
    }

    private external fun nativeOnFrameAvailable(nativePtr: Long)
}