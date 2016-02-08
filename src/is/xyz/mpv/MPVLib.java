package is.xyz.mpv;

// Wrapper for native library

public class MPVLib {

     static {
        String[] libs = { "mpv", "player" };
        for (String lib: libs) {
            System.loadLibrary(lib);
        }
     }

     public static native void init();
     public static native void resize(int width, int height);
     public static native void step();
}
