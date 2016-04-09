package is.xyz.mpv;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

class MPVGlContextFactory implements GLSurfaceView.EGLContextFactory {
    private static final String TAG = "mpv";
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };

        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);
        Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Created ogl es context");
        // MPVLib.initgl();
        // Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Initialized mpv ogl");
        return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display,
                               EGLContext context) {
        Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Destroying mpv gl");
        MPVLib.destroygl();
        Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Destroying ogl es context");
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
            Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
        }
    }
}

class MPVView extends GLSurfaceView {
    private static final String TAG = "mpv";
    private final ThreadLocal<Renderer> muh_renderer = new ThreadLocal<Renderer>() {
        @Override
        protected Renderer initialValue() {
            return new Renderer();
        }
    };

    public MPVView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeGl();
    }

    private void initializeGl() {
        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 3.0 or later backwards-compatible versions.
        Log.w(TAG + " [tid: " + Thread.currentThread().getId() + "]", "Setting EGLContextFactory");
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(3);
        // setPreserveEGLContextOnPause(true);  // TODO: this won't work all the time. we should manually recrete the context in onSurfaceCreated
        setEGLContextFactory(new MPVGlContextFactory());
        setRenderer(muh_renderer.get());
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        final int x = (int) ev.getX(0);
        final int y = (int) ev.getY(0);
        switch(ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                MPVLib.touch_down(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                MPVLib.touch_move(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                MPVLib.touch_up(x, y);
                return true;
        }
        return super.onTouchEvent(ev);
    }



    private static class Renderer implements GLSurfaceView.Renderer {
        public void onDrawFrame(GL10 gl) {
            MPVLib.draw();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            MPVLib.resize(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.w(TAG, "Creating libmpv GL surface");
            MPVLib.initgl();
        }
    }
}
