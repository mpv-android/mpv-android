package is.xyz.mpv;

public interface EventObserver {
    void eventProperty(String property);
    void eventProperty(String property, long value);
    void eventProperty(String property, boolean value);
}
