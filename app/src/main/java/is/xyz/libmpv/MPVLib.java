package is.xyz.libmpv;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * Java wrapper for libmpv library.
 * <p>
 * See <a hred="https://github.com/mpv-player/mpv/blob/master/libmpv/client.h" target="_top">libmpv/client.h</a>.
 */
@SuppressWarnings({"deprecation", "SpellCheckingInspection", "unused"})
public class MPVLib {

    static {
        String[] libs = {"mpv", "player"};
        for (String lib : libs) {
            System.loadLibrary(lib);
        }
    }

    /**
     * Create a new mpv instance and an associated client API handle to control
     * the mpv instance. This instance is in a pre-initialized state,
     * and needs to be initialized to be actually used with most other API
     * functions.
     * <p>
     * Some API functions will return {@link #MPV_ERROR_UNINITIALIZED} in the uninitialized
     * state. You can call mpv_set_property() (or {@link #setPropertyString(String, String)} and
     * other variants, and before mpv 0.21.0 {@link #setOptionString(String, String)} etc.) to set initial
     * options. After this, call {@link #init()} to start the player, and then use
     * e.g. {@link #command(String[])} to start playback of a file.
     * <p>
     * The point of separating handle creation and actual initialization is that
     * you can configure things which can't be changed during runtime.
     */
    public static native void create(Context appctx);

    /**
     * Initialize an uninitialized mpv instance. If the mpv instance is already
     * running, an error is returned.
     * <p>
     * This function needs to be called to make full use of the client API if the
     * client API handle was created with {@link #create(Context)}.
     * <p>
     * Only the following options are required to be set before mpv_initialize():
     * - options which are only read at initialization time:
     * - config
     * - config-dir
     * - input-conf
     * - load-scripts
     * - script
     * - player-operation-mode
     * - input-app-events (OSX)
     * - all encoding mode options
     */
    public static native void init();

    /**
     * This function quits the player, waits until all other clients are destroyed,
     * and also waits for the final termination of the player.
     */
    public static native void destroy();

    /**
     * Attach mpv to an existing Surface for video output.
     *
     * @param surface Surface to attach.
     */
    public static native void attachSurface(Surface surface);

    /**
     * Detach mpv window.
     */
    public static native void detachSurface();

    /**
     * Send a command to the player. Commands are the same as those used in
     * input.conf, except that this function takes parameters in a pre-split
     * form.
     *
     * @param cmd List of strings. Usually, the first item is the command,
     *            and the following items are arguments.
     */
    public static native void command(@NonNull String[] cmd);

    /**
     * Set an option. Note that you can't normally set options during runtime. It
     * works in uninitialized state (see {@link #create(Context)}), and in some cases in at
     * runtime.
     * <p>
     * Note: this is semi-deprecated. For most purposes, this is not needed anymore.
     * Starting with mpv version 0.21.0 (version 1.23) most options can be set
     * with mpv_set_property() (and related functions), and even before
     * mpv_initialize(). In some obscure corner cases, using this function
     * to set options might still be required (see below, and also section
     * "Inconsistencies between options and properties" on the manpage). Once
     * these are resolved, the option setting functions might be fully
     * deprecated.
     * <p>
     * The following options still need to be set either before
     * {@link #create(Context)} with {@link #setPropertyString(String, String)} (or related functions), or
     * with mpv_set_option() (or related functions) at any time:
     * - options shadowed by deprecated properties:
     * - demuxer (property deprecated in 0.21.0)
     * - idle (property deprecated in 0.21.0)
     * - fps (property deprecated in 0.21.0)
     * - cache (property deprecated in 0.21.0)
     * - length (property deprecated in 0.10.0)
     * - audio-samplerate (property deprecated in 0.10.0)
     * - audio-channels (property deprecated in 0.10.0)
     * - audio-format (property deprecated in 0.10.0)
     * - deprecated options shadowed by properties:
     * - chapter (option deprecated in 0.21.0)
     * - playlist-pos (option deprecated in 0.21.0)
     * The deprecated properties were removed in mpv 0.23.0.
     *
     * @param name  Option name. This is the same as on the mpv command line, but without the leading "--".
     * @param value Option value in {@link #MPV_FORMAT_STRING}.
     * @return {@link Error} code
     */
    @Error
    public static native int setOptionString(@NonNull String name, @NonNull String value);

    /**
     * Read the value of the given property.
     * <p>
     * This is equivalent to mpv_get_property() with {@link #MPV_FORMAT_INT64}.
     *
     * @param property The property name.
     * @return Property value
     */
    public static native Integer getPropertyInt(@NonNull String property);

    /**
     * Set a property to a given value.
     * <p>
     * This is like calling mpv_set_property() with {@link #MPV_FORMAT_INT64}.
     *
     * @param property The property name.
     * @param value    Option value
     */
    public static native void setPropertyInt(@NonNull String property, @NonNull Integer value);

    /**
     * Read the value of the given property.
     * <p>
     * This is equivalent to mpv_get_property() with {@link #MPV_FORMAT_DOUBLE}.
     *
     * @param property The property name.
     * @return Property value
     */
    public static native Double getPropertyDouble(@NonNull String property);

    /**
     * Set a property to a given value.
     * <p>
     * This is like calling mpv_set_property() with {@link #MPV_FORMAT_DOUBLE}.
     *
     * @param property The property name.
     * @param value    Option value
     */
    public static native void setPropertyDouble(@NonNull String property, @NonNull Double value);

    /**
     * Read the value of the given property.
     * <p>
     * This is equivalent to mpv_get_property() with {@link #MPV_FORMAT_FLAG}.
     *
     * @param property The property name.
     * @return Property value
     */
    public static native Boolean getPropertyBoolean(@NonNull String property);

    /**
     * Set a property to a given value.
     * <p>
     * This is like calling mpv_set_property() with {@link #MPV_FORMAT_FLAG}.
     *
     * @param property The property name.
     * @param value    Option value
     */
    public static native void setPropertyBoolean(@NonNull String property, @NonNull Boolean value);

    /**
     * Read the value of the given property.
     * <p>
     * This is equivalent to mpv_get_property() with {@link #MPV_FORMAT_STRING}.
     *
     * @param property The property name.
     * @return Property value
     */
    public static native String getPropertyString(@NonNull String property);

    /**
     * Set a property to a given value.
     * <p>
     * This is like calling mpv_set_property() with {@link #MPV_FORMAT_STRING}.
     *
     * @param property The property name.
     * @param value    Option value
     */
    public static native void setPropertyString(@NonNull String property, @NonNull String value);

    /**
     * Get a notification whenever the given property changes. You will receive
     * updates as {@link #MPV_EVENT_PROPERTY_CHANGE} and {@link EventObserver}. Note that this is not very precise:
     * for some properties, it may not send updates even if the property changed.
     * <p>
     * If the property is observed with the format parameter set to {@link #MPV_FORMAT_NONE},
     * you get low-level notifications whether the property may have changed, and
     * the data member in mpv_event_property will be unset. With this mode, you
     * will have to determine yourself whether the property really changed. On the
     * other hand, this mechanism can be faster and uses less resources.
     * <p>
     * Observing a property that doesn't exist is allowed. (Although it may still
     * cause some sporadic change events.)
     * <p>
     * Keep in mind that you will get change notifications even if you change a
     * property yourself. Try to avoid endless feedback loops, which could happen
     * if you react to the change notifications triggered by your own change.
     *
     * @param property The property name.
     * @param format   See {@link Format}. Can be {@link #MPV_FORMAT_NONE} to omit values
     *                 from the change events.
     */
    public static native void observeProperty(@NonNull String property, @Format int format);

    private static final List<EventObserver> observers = new ArrayList<>();

    /**
     * Add a {@link EventObserver}.
     *
     * @param observer {@link EventObserver}
     */
    public static void addObserver(EventObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    /**
     * Remove a {@link EventObserver}.
     *
     * @param observer {@link EventObserver}
     */
    public static void removeObserver(EventObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    /**
     * See {@link EventObserver#eventProperty(String)}.
     */
    public static void eventProperty(String property) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventProperty(property);
        }
    }

    /**
     * See {@link EventObserver#eventProperty(String, String)}.
     */
    public static void eventProperty(String property, String value) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventProperty(property, value);
        }
    }

    /**
     * See {@link EventObserver#eventProperty(String, boolean)}.
     */
    public static void eventProperty(String property, boolean value) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventProperty(property, value);
        }
    }

    /**
     * See {@link EventObserver#eventProperty(String, long)}.
     */
    public static void eventProperty(String property, long value) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventProperty(property, value);
        }
    }
    /**
     * See {@link EventObserver#eventProperty(String, double)}.
     */
    public static void eventProperty(String property, double value) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventProperty(property, value);
        }
    }

    /**
     * See {@link EventObserver#event(int)}.
     */
    public static void event(@Event int eventId) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.event(eventId);
        }
    }

    /**
     * See {@link EventObserver#eventEndFile(int, int)}.
     */
    public static void eventEndFile(@Reason int reason, @Error int error) {
        synchronized (observers) {
            for (EventObserver o : observers)
                o.eventEndFile(reason, error);
        }
    }

    private static final List<LogObserver> log_observers = new ArrayList<>();

    /**
     * Add a {@link LogObserver}.
     *
     * @param observer {@link LogObserver}
     */
    public static void addLogObserver(LogObserver observer) {
        synchronized (log_observers) {
            log_observers.add(observer);
        }
    }

    /**
     * Remove a {@link LogObserver}.
     *
     * @param observer {@link LogObserver}
     */
    public static void removeLogObserver(LogObserver observer) {
        synchronized (log_observers) {
            log_observers.remove(observer);
        }
    }

    /**
     * See {@link LogObserver#logMessage(String, int, String)}.
     */
    public static void logMessage(String prefix, @LogLevel int level, String text) {
        synchronized (log_observers) {
            for (LogObserver o : log_observers)
                o.logMessage(prefix, level, text);
        }
    }

    /**
     * Receives value changes for a property or event that is being observed.
     */
    public interface EventObserver {
        /**
         * Receives value changes for a property in {@link Format} {@link #MPV_FORMAT_NONE}.
         *
         * @param property The property name.
         */
        void eventProperty(@NonNull String property);

        /**
         * Receives value changes for a property in {@link Format} {@link #MPV_FORMAT_STRING}.
         *
         * @param property The property name.
         * @param value    The property value.
         */
        void eventProperty(@NonNull String property, @NonNull String value);

        /**
         * Receives value changes for a property in {@link Format} {@link #MPV_FORMAT_FLAG}.
         *
         * @param property The property name.
         * @param value    The property value.
         */
        void eventProperty(@NonNull String property, boolean value);

        /**
         * Receives value changes for a property in {@link Format} {@link #MPV_FORMAT_INT64}.
         *
         * @param property The property name.
         * @param value    The property value.
         */
        void eventProperty(@NonNull String property, long value);

        /**
         * Receives value changes for a property in {@link Format} {@link #MPV_FORMAT_DOUBLE}.
         *
         * @param property The property name.
         * @param value    The property value.
         */
        void eventProperty(@NonNull String property, double value);

        /**
         * Receives a {@link Event}.
         *
         * @param eventId The {@link Event}
         */
        void event(@Event int eventId);

        /**
         * Receives a {@link #MPV_EVENT_END_FILE} event
         *
         * @param reason The {@link Reason}
         * @param error  If reason=={@link #MPV_END_FILE_REASON_ERROR}, this contains a mpv error code
         *               (one of {@link Error}) giving an approximate reason why playback
         *               failed. In other cases, this field is {@link #MPV_ERROR_SUCCESS} (no error).
         */
        void eventEndFile(@Reason int reason, @Error int error);
    }

    /**
     * Receives messages with mpv_request_log_messages().
     */
    public interface LogObserver {

        /**
         * These are the messages the command line player prints to the terminal.
         *
         * @param prefix The module prefix, identifies the sender of the message.
         * @param level  The log level as string. See msg.log for possible log level names.
         * @param text   The log message. The text will end with a newline character.
         *               Sometimes it can contain multiple lines.
         */
        void logMessage(@NonNull String prefix, @LogLevel int level, @NonNull String text);
    }

    /**
     * Data format for options and properties. The API functions to get/set
     * properties and options support multiple formats.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MPV_FORMAT_NONE,
        MPV_FORMAT_STRING,
        MPV_FORMAT_OSD_STRING,
        MPV_FORMAT_FLAG,
        MPV_FORMAT_INT64,
        MPV_FORMAT_DOUBLE,
        MPV_FORMAT_NODE,
        MPV_FORMAT_NODE_ARRAY,
        MPV_FORMAT_NODE_MAP,
        MPV_FORMAT_BYTE_ARRAY
    })
    public @interface Format {}

    /**
     * Invalid. Sometimes used for empty values. This is always defined to 0,
     * so a normal 0-init of mpv_format (or e.g. mpv_node) is guaranteed to set
     * this it to MPV_FORMAT_NONE (which makes some things saner as consequence).
     */
    public static final int MPV_FORMAT_NONE = 0;

    /**
     * The basic type is char*. It returns the raw property string, like
     * using ${=property} in input.conf (see input.rst).
     * <p>
     * NULL isn't an allowed value.
     */
    public static final int MPV_FORMAT_STRING = 1;

    /**
     * The basic type is char*. It returns the OSD property string, like
     * using ${property} in input.conf (see input.rst). In many cases, this
     * is the same as the raw string, but in other cases it's formatted for
     * display on OSD. It's intended to be human readable. Do not attempt to
     * parse these strings.
     * <p>
     * Only valid when doing read access. The rest works like {@link #MPV_FORMAT_STRING}.
     */
    public static final int MPV_FORMAT_OSD_STRING = 2;

    /**
     * The basic type is int. The only allowed values are 0 ("no")
     * and 1 ("yes").
     */
    public static final int MPV_FORMAT_FLAG = 3;

    /**
     * The basic type is int64_t.
     */
    public static final int MPV_FORMAT_INT64 = 4;

    /**
     * The basic type is double.
     */
    public static final int MPV_FORMAT_DOUBLE = 5;

    /**
     * The type is mpv_node.
     * <p>
     * For reading, you usually would pass a pointer to a stack-allocated
     * mpv_node value to mpv, and when you're done you call
     * mpv_free_node_contents(node).
     * You're expected not to write to the data - if you have to, copy it
     * first (which you have to do manually).
     * <p>
     * For writing, you construct your own mpv_node, and pass a pointer to the
     * API. The API will never write to your data (and copy it if needed), so
     * you're free to use any form of allocation or memory management you like.
     */
    public static final int MPV_FORMAT_NODE = 6;

    /**
     * Used with mpv_node only. Can usually not be used directly.
     */
    public static final int MPV_FORMAT_NODE_ARRAY = 7;

    /**
     * See {@link #MPV_FORMAT_NODE_ARRAY}.
     */
    public static final int MPV_FORMAT_NODE_MAP = 8;

    /**
     * A raw, untyped byte array. Only used only with mpv_node, and only in
     * some very specific situations. (Some commands use it.)
     */
    public static final int MPV_FORMAT_BYTE_ARRAY = 9;

    /**
     * Data format for events. In general, the API user should run an event
     * loop in order to receive events.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MPV_EVENT_NONE,
        MPV_EVENT_SHUTDOWN,
        MPV_EVENT_LOG_MESSAGE,
        MPV_EVENT_GET_PROPERTY_REPLY,
        MPV_EVENT_SET_PROPERTY_REPLY,
        MPV_EVENT_COMMAND_REPLY,
        MPV_EVENT_START_FILE,
        MPV_EVENT_END_FILE,
        MPV_EVENT_FILE_LOADED,
        MPV_EVENT_TRACKS_CHANGED,
        MPV_EVENT_TRACK_SWITCHED,
        MPV_EVENT_IDLE,
        MPV_EVENT_PAUSE,
        MPV_EVENT_UNPAUSE,
        MPV_EVENT_TICK,
        MPV_EVENT_SCRIPT_INPUT_DISPATCH,
        MPV_EVENT_CLIENT_MESSAGE,
        MPV_EVENT_VIDEO_RECONFIG,
        MPV_EVENT_AUDIO_RECONFIG,
        MPV_EVENT_METADATA_UPDATE,
        MPV_EVENT_SEEK,
        MPV_EVENT_PLAYBACK_RESTART,
        MPV_EVENT_PROPERTY_CHANGE,
        MPV_EVENT_CHAPTER_CHANGE,
        MPV_EVENT_QUEUE_OVERFLOW,
        MPV_EVENT_HOOK
    })
    public @interface Event {}

    /**
     * Nothing happened. Happens on timeouts or sporadic wakeups.
     */
    public static final int MPV_EVENT_NONE = 0;

    /**
     * Happens when the player quits. The player enters a state where it tries
     * to disconnect all clients. Most requests to the player will fail, and
     * the client should react to this and quit with {@link #destroy()} as
     * soon as possible.
     */
    public static final int MPV_EVENT_SHUTDOWN = 1;

    /**
     * See {@link LogObserver}
     */
    public static final int MPV_EVENT_LOG_MESSAGE = 2;

    /**
     * Reply to a mpv_get_property_async() request.
     * See {@link EventObserver}.
     */
    public static final int MPV_EVENT_GET_PROPERTY_REPLY = 3;

    /**
     * Reply to a mpv_set_property_async() request.
     * (Unlike MPV_EVENT_GET_PROPERTY, mpv_event_property is not used.)
     */
    public static final int MPV_EVENT_SET_PROPERTY_REPLY = 4;

    /**
     * Reply to a mpv_command_async() or mpv_command_node_async() request.
     * See also {@link EventObserver} and {@link #command(String[])}.
     */
    public static final int MPV_EVENT_COMMAND_REPLY = 5;

    /**
     * Notification before playback start of a file (before the file is loaded).
     * See also {@link EventObserver}.
     */
    public static final int MPV_EVENT_START_FILE = 6;

    /**
     * Notification after playback end (after the file was unloaded).
     * See also {@link EventObserver}.
     */
    public static final int MPV_EVENT_END_FILE = 7;

    /**
     * Notification when the file has been loaded (headers were read etc.), and
     * decoding starts.
     */
    public static final int MPV_EVENT_FILE_LOADED = 8;

    /**
     * The list of video/audio/subtitle tracks was changed. (E.g. a new track
     * was found. This doesn't necessarily indicate a track switch; for this,
     * {@link #MPV_EVENT_TRACK_SWITCHED} is used.)
     *
     * @deprecated This is equivalent to using {@link #observeProperty(String, int)} on the
     * "track-list" property. The event is redundant, and might
     * be removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_TRACKS_CHANGED = 9;

    /**
     * A video/audio/subtitle track was switched on or off.
     *
     * @deprecated This is equivalent to using {@link #observeProperty(String, int)} on the
     * "vid", "aid", and "sid" properties. The event is redundant,
     * and might be removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_TRACK_SWITCHED = 10;

    /**
     * Idle mode was entered. In this mode, no file is played, and the playback
     * core waits for new commands. (The command line player normally quits
     * instead of entering idle mode, unless --idle was specified. If mpv
     * was started with {@link #create(Context)}, idle mode is enabled by default.)
     *
     * @deprecated This is equivalent to using {@link #observeProperty(String, int)} on the
     * "idle-active" property. The event is redundant, and might be
     * removed in the far future. As a further warning, this event
     * is not necessarily sent at the right point anymore (at the
     * start of the program), while the property behaves correctly.
     */
    @Deprecated
    public static final int MPV_EVENT_IDLE = 11;

    /**
     * Playback was paused. This indicates the user pause state.
     * <p>
     * The user pause state is the state the user requested (changed with the
     * "pause" property). There is an internal pause state too, which is entered
     * if e.g. the network is too slow (the "core-idle" property generally
     * indicates whether the core is playing or waiting).
     * <p>
     * This event is sent whenever any pause states change, not only the user
     * state. You might get multiple events in a row while these states change
     * independently. But the event ID sent always indicates the user pause
     * state.
     * <p>
     * If you don't want to deal with this, use {@link #observeProperty(String, int)} on the
     * "pause" property and ignore MPV_EVENT_PAUSE/UNPAUSE. Likewise, the
     * "core-idle" property tells you whether video is actually playing or not.
     *
     * @deprecated The event is redundant with {@link #observeProperty(String, int)} as
     * mentioned above, and might be removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_PAUSE = 12;

    /**
     * Playback was unpaused. See ${@link #MPV_EVENT_PAUSE} for not so obvious details.
     *
     * @deprecated The event is redundant with {@link #observeProperty(String, int)} as
     * explained in the ${@link #MPV_EVENT_PAUSE} comments, and might be
     * removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_UNPAUSE = 13;

    /**
     * Sent every time after a video frame is displayed. Note that currently,
     * this will be sent in lower frequency if there is no video, or playback
     * is paused - but that will be removed in the future, and it will be
     * restricted to video frames only.
     *
     * @deprecated Use {@link #observeProperty(String, int)} with relevant properties instead
     * (such as "playback-time").
     */
    @Deprecated
    public static final int MPV_EVENT_TICK = 14;

    /**
     * @deprecated This was used internally with the internal "script_dispatch"
     * command to dispatch keyboard and mouse input for the OSC.
     * It was never useful in general and has been completely
     * replaced with "script-binding".
     * This event never happens anymore, and is included in this
     * header only for compatibility.
     */
    @Deprecated
    public static final int MPV_EVENT_SCRIPT_INPUT_DISPATCH = 15;

    /**
     * Triggered by the script-message input command. The command uses the
     * first argument of the command as client name (see mpv_client_name()) to
     * dispatch the message, and passes along all arguments starting from the
     * second argument as strings.
     * See also {@link EventObserver} and mpv_event_client_message.
     */
    public static final int MPV_EVENT_CLIENT_MESSAGE = 16;

    /**
     * Happens after video changed in some way. This can happen on resolution
     * changes, pixel format changes, or video filter changes. The event is
     * sent after the video filters and the VO are reconfigured. Applications
     * embedding a mpv window should listen to this event in order to resize
     * the window if needed.
     * Note that this event can happen sporadically, and you should check
     * yourself whether the video parameters really changed before doing
     * something expensive.
     */
    public static final int MPV_EVENT_VIDEO_RECONFIG = 17;

    /**
     * Similar to {@link #MPV_EVENT_VIDEO_RECONFIG}. This is relatively uninteresting,
     * because there is no such thing as audio output embedding.
     */
    public static final int MPV_EVENT_AUDIO_RECONFIG = 18;

    /**
     * Happens when metadata (like file tags) is possibly updated. (It's left
     * unspecified whether this happens on file start or only when it changes
     * within a file.)
     *
     * @deprecated This is equivalent to using {@link #observeProperty(String, int)} on the
     * "metadata" property. The event is redundant, and might
     * be removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_METADATA_UPDATE = 19;

    /**
     * Happens when a seek was initiated. Playback stops. Usually it will
     * resume with {@link #MPV_EVENT_PLAYBACK_RESTART} as soon as the seek is finished.
     */
    public static final int MPV_EVENT_SEEK = 20;

    /**
     * There was a discontinuity of some sort (like a seek), and playback
     * was reinitialized. Usually happens on start of playback and after
     * seeking. The main purpose is allowing the client to detect when a seek
     * request is finished.
     */
    public static final int MPV_EVENT_PLAYBACK_RESTART = 21;

    /**
     * Event sent due to {@link #observeProperty(String, int)}.
     * See also {@link EventObserver} and {@link #observeProperty(String, int)}.
     */
    public static final int MPV_EVENT_PROPERTY_CHANGE = 22;

    /**
     * Happens when the current chapter changes.
     *
     * @deprecated This is equivalent to using {@link #observeProperty(String, int)} on the
     * "chapter" property. The event is redundant, and might
     * be removed in the far future.
     */
    @Deprecated
    public static final int MPV_EVENT_CHAPTER_CHANGE = 23;

    /**
     * Happens if the internal per-mpv_handle ringbuffer overflows, and at
     * least 1 event had to be dropped. This can happen if the client doesn't
     * read the event queue quickly enough with mpv_wait_event(), or if the
     * client makes a very large number of asynchronous calls at once.
     * <p>
     * Event delivery will continue normally once this event was returned
     * (this forces the client to empty the queue completely).
     */
    public static final int MPV_EVENT_QUEUE_OVERFLOW = 24;

    /**
     * Triggered if a hook handler was registered with mpv_hook_add(), and the
     * hook is invoked. If you receive this, you must handle it, and continue
     * the hook with mpv_hook_continue().
     * See also {@link EventObserver} and mpv_event_hook.
     */
    public static final int MPV_EVENT_HOOK = 25;

    /**
     * Numeric log levels. The lower the number, the more important the message is.
     * {@link #MPV_LOG_LEVEL_NONE} is never used when receiving messages. The string in
     * the comment after the value is the name of the log level as used for the
     * {@link #logMessage(String, int, String)} function.
     * Unused numeric values are unused, but reserved for future use.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MPV_LOG_LEVEL_NONE,
        MPV_LOG_LEVEL_FATAL,
        MPV_LOG_LEVEL_ERROR,
        MPV_LOG_LEVEL_WARN,
        MPV_LOG_LEVEL_INFO,
        MPV_LOG_LEVEL_V,
        MPV_LOG_LEVEL_DEBUG,
        MPV_LOG_LEVEL_TRACE
    })
    public @interface LogLevel {}

    /**
     * "no" - disable absolutely all messages.
     */
    public static final int MPV_LOG_LEVEL_NONE = 0;

    /**
     * "fatal" - critical/aborting errors.
     */
    public static final int MPV_LOG_LEVEL_FATAL = 10;

    /**
     * "error" - simple errors.
     */
    public static final int MPV_LOG_LEVEL_ERROR = 20;

    /**
     * "warn" - possible problems.
     */
    public static final int MPV_LOG_LEVEL_WARN = 30;

    /**
     * "info" - informational message.
     */
    public static final int MPV_LOG_LEVEL_INFO = 40;

    /**
     * "v" - noisy informational message.
     */
    public static final int MPV_LOG_LEVEL_V = 50;

    /**
     * "debug" - very noisy technical information.
     */
    public static final int MPV_LOG_LEVEL_DEBUG = 60;

    /**
     * "trace" - extremely noisy.
     */
    public static final int MPV_LOG_LEVEL_TRACE = 70;

    /**
     * Reason for {@link #MPV_EVENT_END_FILE}.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MPV_END_FILE_REASON_EOF,
        MPV_END_FILE_REASON_STOP,
        MPV_END_FILE_REASON_QUIT,
        MPV_END_FILE_REASON_ERROR,
        MPV_END_FILE_REASON_REDIRECT
    })
    public @interface Reason {}

    /**
     * The end of file was reached. Sometimes this may also happen on
     * incomplete or corrupted files, or if the network connection was
     * interrupted when playing a remote file. It also happens if the
     * playback range was restricted with --end or --frames or similar.
     */
    public static final int MPV_END_FILE_REASON_EOF = 0;

    /**
     * Playback was stopped by an external action (e.g. playlist controls).
     */
    public static final int MPV_END_FILE_REASON_STOP = 2;

    /**
     * Playback was stopped by the quit command or player shutdown.
     */
    public static final int MPV_END_FILE_REASON_QUIT = 3;

    /**
     * Some kind of error happened that lead to playback abort. Does not
     * necessarily happen on incomplete or broken files (in these cases, both
     * MPV_END_FILE_REASON_ERROR or MPV_END_FILE_REASON_EOF are possible).
     * <p>
     * mpv_event_end_file.error will be set.
     */
    public static final int MPV_END_FILE_REASON_ERROR = 4;

    /**
     * The file was a playlist or similar. When the playlist is read, its
     * entries will be appended to the playlist after the entry of the current
     * file, the entry of the current file is removed, and a MPV_EVENT_END_FILE
     * event is sent with reason set to MPV_END_FILE_REASON_REDIRECT. Then
     * playback continues with the playlist contents.
     */
    public static final int MPV_END_FILE_REASON_REDIRECT = 5;

    /**
     * List of error codes than can be returned by API functions. 0 and positive
     * return values always mean success, negative values are always errors.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MPV_ERROR_SUCCESS,
        MPV_ERROR_EVENT_QUEUE_FULL,
        MPV_ERROR_NOMEM,
        MPV_ERROR_UNINITIALIZED,
        MPV_ERROR_INVALID_PARAMETER,
        MPV_ERROR_OPTION_NOT_FOUND,
        MPV_ERROR_OPTION_FORMAT,
        MPV_ERROR_OPTION_ERROR,
        MPV_ERROR_PROPERTY_NOT_FOUND,
        MPV_ERROR_PROPERTY_FORMAT,
        MPV_ERROR_PROPERTY_UNAVAILABLE,
        MPV_ERROR_PROPERTY_ERROR,
        MPV_ERROR_COMMAND,
        MPV_ERROR_LOADING_FAILED,
        MPV_ERROR_AO_INIT_FAILED,
        MPV_ERROR_VO_INIT_FAILED,
        MPV_ERROR_NOTHING_TO_PLAY,
        MPV_ERROR_UNKNOWN_FORMAT,
        MPV_ERROR_UNSUPPORTED,
        MPV_ERROR_NOT_IMPLEMENTED,
        MPV_ERROR_GENERIC
    })
    public @interface Error {}

    /**
     * No error happened (used to signal successful operation).
     * Keep in mind that many API functions returning error codes can also
     * return positive values, which also indicate success. API users can
     * hardcode the fact that {@literal ">= 0"} means success.
     */
    public static final int MPV_ERROR_SUCCESS = 0;

    /**
     * The event ringbuffer is full. This means the client is choked, and can't
     * receive any events. This can happen when too many asynchronous requests
     * have been made, but not answered. Probably never happens in practice,
     * unless the mpv core is frozen for some reason, and the client keeps
     * making asynchronous requests. (Bugs in the client API implementation
     * could also trigger this, e.g. if events become "lost".)
     */
    public static final int MPV_ERROR_EVENT_QUEUE_FULL = -1;

    /**
     * Memory allocation failed.
     */
    public static final int MPV_ERROR_NOMEM = -2;

    /**
     * The mpv core wasn't configured and initialized yet. See the notes in
     * mpv_create().
     */
    public static final int MPV_ERROR_UNINITIALIZED = -3;

    /**
     * Generic catch-all error if a parameter is set to an invalid or
     * unsupported value. This is used if there is no better error code.
     */
    public static final int MPV_ERROR_INVALID_PARAMETER = -4;

    /**
     * Trying to set an option that doesn't exist.
     */
    public static final int MPV_ERROR_OPTION_NOT_FOUND = -5;

    /**
     * Trying to set an option using an unsupported MPV_FORMAT.
     */
    public static final int MPV_ERROR_OPTION_FORMAT = -6;

    /**
     * Setting the option failed. Typically this happens if the provided option
     * value could not be parsed.
     */
    public static final int MPV_ERROR_OPTION_ERROR = -7;

    /**
     * The accessed property doesn't exist.
     */
    public static final int MPV_ERROR_PROPERTY_NOT_FOUND = -8;

    /**
     * Trying to set or get a property using an unsupported MPV_FORMAT.
     */
    public static final int MPV_ERROR_PROPERTY_FORMAT = -9;

    /**
     * The property exists, but is not available. This usually happens when the
     * associated subsystem is not active, e.g. querying audio parameters while
     * audio is disabled.
     */
    public static final int MPV_ERROR_PROPERTY_UNAVAILABLE = -10;

    /**
     * Error setting or getting a property.
     */
    public static final int MPV_ERROR_PROPERTY_ERROR = -11;

    /**
     * General error when running a command with mpv_command and similar.
     */
    public static final int MPV_ERROR_COMMAND = -12;

    /**
     * Generic error on loading (usually used with mpv_event_end_file.error).
     */
    public static final int MPV_ERROR_LOADING_FAILED = -13;

    /**
     * Initializing the audio output failed.
     */
    public static final int MPV_ERROR_AO_INIT_FAILED = -14;

    /**
     * Initializing the video output failed.
     */
    public static final int MPV_ERROR_VO_INIT_FAILED = -15;

    /**
     * There was no audio or video data to play. This also happens if the
     * file was recognized, but did not contain any audio or video streams,
     * or no streams were selected.
     */
    public static final int MPV_ERROR_NOTHING_TO_PLAY = -16;

    /**
     * When trying to load the file, the file format could not be determined,
     * or the file was too broken to open it.
     */
    public static final int MPV_ERROR_UNKNOWN_FORMAT = -17;

    /**
     * Generic error for signaling that certain system requirements are not
     * fulfilled.
     */
    public static final int MPV_ERROR_UNSUPPORTED = -18;

    /**
     * The API function which was called is a stub only.
     */
    public static final int MPV_ERROR_NOT_IMPLEMENTED = -19;

    /**
     * Unspecified error.
     */
    public static final int MPV_ERROR_GENERIC = -20;
}