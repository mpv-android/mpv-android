package is.xyz.mpv;

// Wrapper for native library

import android.util.EventLog;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
     public static native boolean getpropertyboolean(String property);
     public static native void observeProperty(String property, int format);

     private static List<EventObserver> observers = new ArrayList<>();

     public static void addObserver(EventObserver o) {
          observers.add(o);
     }

     public static void clearObservers() {
          observers.clear();
     }

     public static void eventProperty(String property, long value) {
          for (EventObserver o : observers)
               o.eventProperty(property, value);
     }

     public static void eventProperty(String property, boolean value) {
          for (EventObserver o : observers)
               o.eventProperty(property, value);
     }

     public static void eventProperty(String property) {
          for (EventObserver o : observers)
               o.eventProperty(property);
     }

     public enum mpvFormat {
          MPV_FORMAT_NONE(0),
          MPV_FORMAT_STRING(1),
          MPV_FORMAT_OSD_STRING(2),
          MPV_FORMAT_FLAG(3),
          MPV_FORMAT_INT64(4),
          MPV_FORMAT_DOUBLE(5),
          MPV_FORMAT_NODE(6),
          MPV_FORMAT_NODE_ARRAY(7),
          MPV_FORMAT_NODE_MAP(8),
          MPV_FORMAT_BYTE_ARRAY(9);

          private int value;

          mpvFormat(int value) {
               this.value = value;
          }

          public int getValue() {
               return value;
          }
     }
}
