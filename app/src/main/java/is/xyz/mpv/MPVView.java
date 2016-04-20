package is.xyz.mpv;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MPVView extends GLSurfaceView {
    private static final String TAG = "mpv";
    private Renderer myRenderer;

    public MPVView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(String configDir) {
        MPVLib.prepareEnv();
        MPVLib.setconfigdir(configDir);
        MPVLib.createLibmpvContext();
        MPVLib.initializeLibmpv();
        MPVLib.setLibmpvOptions();
        observeProperties();
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
                MPVLib.destroygl();
            }
        });
        super.onPause();
    }

    // Called when back button is pressed, or app is shutting down
    public void destroy() {
        // At this point Renderer is already dead so it won't call step/draw, as such it's safe to free mpv resources
        MPVLib.clearObservers();
        MPVLib.destroy();
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        final int x = (int) ev.getX(0);
        final int y = (int) ev.getY(0);
        final int action = ev.getActionMasked();

        switch(action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                queueEvent(new Runnable() {
                    // This method will be called on the rendering
                    // thread:
                    public void run() {
                        myRenderer.handleTouchEvent(x, y, action);
                    }
                });
                return true;
            default:
                return super.onTouchEvent(ev);
        }
    }

    public void observeProperties() {
        HashMap<String, MPVLib.mpvFormat> p = new HashMap<>();
        p.put("time-pos", MPVLib.mpvFormat.MPV_FORMAT_INT64);
        // p.put("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG);
        for (Map.Entry<String, MPVLib.mpvFormat> property : p.entrySet())
            MPVLib.observeProperty(property.getKey(), property.getValue().getValue());
    }

    public void addObserver(EventObserver o) {
        MPVLib.addObserver(o);
    }

    public int getDuration() {
        return MPVLib.getpropertyint("duration");
    }

    public int getTimePos() {
        return MPVLib.getpropertyint("time-pos");
    }

    public void setTimePos(int progress) {
        MPVLib.setpropertyint("time-pos", progress);
    }

    public void cyclePause() {
        MPVLib.command(new String[]{"cycle", "pause"});
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
            MPVLib.initgl();
            if (filePath != null) {
                Log.w(TAG, "Giving command to open file!");
                MPVLib.command(new String[]{"loadfile", filePath});
            } else {
                Log.w(TAG, "Blergh no file path");
            }
        }

        public void handleTouchEvent(int x, int y, final int event) {
            Log.w(TAG, "Das touch event: x="+x+"y="+y+", event="+event);
            switch(event) {
                case MotionEvent.ACTION_DOWN:
                    MPVLib.touch_down(x, y);
                case MotionEvent.ACTION_MOVE:
                    MPVLib.touch_move(x, y);
                case MotionEvent.ACTION_UP:
                    MPVLib.touch_up(x, y);
            }
        }

        public void setFilePath(String file_path) {
            filePath = file_path;
        }
    }
}
