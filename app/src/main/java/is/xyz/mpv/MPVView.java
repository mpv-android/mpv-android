package is.xyz.mpv;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

class MPVView extends GLSurfaceView {
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
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(3);
        // setPreserveEGLContextOnPause(true);  // TODO: this won't work all the time. we should manually recrete the context in onSurfaceCreated
        setRenderer(muh_renderer.get());
    }

    @Override public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                muh_renderer.get().destroy_gl();
            }
        });

        super.onPause();
    }

    @Override public void onResume() {
        /*
        queueEvent(new Runnable() {
            @Override
            public void run() {
                muh_renderer.get().init_gl();
            }
        });*/

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
            MPVLib.step();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            MPVLib.resize(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            MPVLib.initgl();
        }

        public void destroy_gl() {
            MPVLib.destroygl();
        }

        public void init_gl() {
            MPVLib.initgl();
        }
    }
}
