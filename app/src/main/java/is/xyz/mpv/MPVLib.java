package is.xyz.mpv;

// Wrapper for native library

public class MPVLib {

     static {
        String[] libs = { "mpv", "player" };
        for (String lib: libs) {
            System.loadLibrary(lib);
        }
     }

     public static native void prepareEnv();
     public static native void createLibmpvContext();
     public static native void initializeLibmpv();
     public static native void setLibmpvOptions();
     public static native void initgl();
     public static native void destroy();
     public static native void destroygl();
     public static native void command(String[] cmd);
     public static native void resize(int width, int height);
     public static native void draw();
     public static native void step();
     public static native void play();
     public static native void pause();
     public static native void touch_down(int x, int y);
     public static native void touch_move(int x, int y);
     public static native void touch_up(int x, int y);
     public static native void setconfigdir(String path);
     public static native int getpropertyint(String property);
     public static native void setpropertyint(String property, int value);
}
