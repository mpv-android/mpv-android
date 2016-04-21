package is.xyz.mpv;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MPVView extends GLSurfaceView {
    private static final String TAG = "mpv";
    private Renderer myRenderer;

    public MPVView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(String configDir) {
        MPVLib.create();
        MPVLib.setOptionString("config", "yes");
        MPVLib.setOptionString("config-dir", configDir);
        MPVLib.init();
        initOptions();
        observeProperties();
    }

    public void initOptions() {
        MPVLib.setOptionString("hwdec", "mediacodec");
        MPVLib.setOptionString("vo", "opengl-cb");
        MPVLib.setOptionString("ao", "opensles");
    }

    public void playFile(String filePath) {
        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 3.0 or later backwards-compatible versions.
        Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Setting EGLContextFactory");
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(2);
        // setPreserveEGLContextOnPause(true);  // TODO: this won't work all the time. we should manually recrete the context in onSurfaceCreated
        myRenderer = new Renderer();
        myRenderer.setFilePath(filePath);
        setRenderer(myRenderer);
    }

    @Override public void onPause() {
        queueEvent(new Runnable() {
            // This method will be called on the rendering
            // thread:
            public void run() {
                MPVLib.destroyGL();
            }
        });
        pause();
        super.onPause();
    }

    // Called when back button is pressed, or app is shutting down
    public void destroy() {
        // At this point Renderer is already dead so it won't call step/draw, as such it's safe to free mpv resources
        MPVLib.clearObservers();
        MPVLib.destroy();
    }

    public void observeProperties() {
        HashMap<String, MPVLib.mpvFormat> p = new HashMap<>();
        p.put("time-pos", MPVLib.mpvFormat.MPV_FORMAT_INT64);
        p.put("duration", MPVLib.mpvFormat.MPV_FORMAT_INT64);
        p.put("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG);
        for (Map.Entry<String, MPVLib.mpvFormat> property : p.entrySet())
            MPVLib.observeProperty(property.getKey(), property.getValue().getValue());
    }

    public void addObserver(EventObserver o) {
        MPVLib.addObserver(o);
    }

    public boolean isPaused() {
        return MPVLib.getPropertyBoolean("pause");
    }

    public int getDuration() {
        return MPVLib.getPropertyInt("duration");
    }

    public int getTimePos() {
        return MPVLib.getPropertyInt("time-pos");
    }

    public void setTimePos(int progress) {
        MPVLib.setPropertyInt("time-pos", progress);
    }

    public void pause() {
        MPVLib.setPropertyBoolean("pause", true);
    }

    public void cyclePause() {
        MPVLib.command(new String[]{"cycle", "pause"});
    }

    public void cycleAudio() {
        MPVLib.command(new String[]{"cycle", "audio"});
    }

    public void cycleSub() {
        MPVLib.command(new String[]{"cycle", "sub"});
    }

    public void cycleHwdec() {
        String next = isHwdecActive() ? "no" : "mediacodec";
        MPVLib.setPropertyString("hwdec", next);
    }

    public boolean isHwdecActive() {
        return MPVLib.getPropertyBoolean("hwdec-active");
    }

    private static class Renderer implements GLSurfaceView.Renderer {
        private String filePath = null;

        public void onDrawFrame(GL10 gl) {
            MPVLib.step();
            MPVLib.draw();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            MPVLib.resize(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.w(TAG, "Creating libmpv GL surface");
            MPVLib.initGL();
            if (filePath != null) {
                MPVLib.command(new String[]{"loadfile", filePath});
                filePath = null;
            } else {
                // Get here when user goes to home screen and then returns to the app
                // mpv disables video output when opengl context is destroyed, enable it back
                MPVLib.setPropertyInt("vid", 1);
            }
        }

        public void setFilePath(String file_path) {
            filePath = file_path;
        }
    }
}
