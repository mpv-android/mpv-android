package `is`.xyz.mpv

// Wrapper for native library

import java.util.*

object MPVLib {

    init {
        val libs = arrayOf("mpv", "player")
        for (lib in libs) {
            System.loadLibrary(lib)
        }
    }

    external fun create()
    external fun init()
    external fun destroy()
    external fun initGL()
    external fun destroyGL()

    external fun resize(width: Int, height: Int)
    external fun draw()
    external fun step()

    @JvmStatic external fun command(cmd: Array<String>)

    external fun setOptionString(name: String, value: String): Int

    external fun getPropertyInt(property: String): Int?
    external fun setPropertyInt(property: String, value: Int?)
    external fun getPropertyBoolean(property: String): Boolean?
    external fun setPropertyBoolean(property: String, value: Boolean?)
    external fun getPropertyString(property: String): String
    external fun setPropertyString(property: String, value: String)

    external fun observeProperty(property: String, format: Int)

    private val observers = ArrayList<EventObserver>()

    fun addObserver(o: EventObserver) {
        observers.add(o)
    }

    fun clearObservers() {
        observers.clear()
    }

    fun eventProperty(property: String, value: Long) {
        for (o in observers)
            o.eventProperty(property, value)
    }

    fun eventProperty(property: String, value: Boolean) {
        for (o in observers)
            o.eventProperty(property, value)
    }

    fun eventProperty(property: String, value: String) {
        for (o in observers)
            o.eventProperty(property)
    }

    fun eventProperty(property: String) {
        for (o in observers)
            o.eventProperty(property)
    }

    fun event(eventId: Int) {
        for (o in observers)
            o.event(eventId)
    }

    object mpvFormat {
        val MPV_FORMAT_NONE = 0
        val MPV_FORMAT_STRING = 1
        val MPV_FORMAT_OSD_STRING = 2
        val MPV_FORMAT_FLAG = 3
        val MPV_FORMAT_INT64 = 4
        val MPV_FORMAT_DOUBLE = 5
        val MPV_FORMAT_NODE = 6
        val MPV_FORMAT_NODE_ARRAY = 7
        val MPV_FORMAT_NODE_MAP = 8
        val MPV_FORMAT_BYTE_ARRAY = 9
    }

    object mpvEventId {
        val MPV_EVENT_NONE = 0
        val MPV_EVENT_SHUTDOWN = 1
        val MPV_EVENT_LOG_MESSAGE = 2
        val MPV_EVENT_GET_PROPERTY_REPLY = 3
        val MPV_EVENT_SET_PROPERTY_REPLY = 4
        val MPV_EVENT_COMMAND_REPLY = 5
        val MPV_EVENT_START_FILE = 6
        val MPV_EVENT_END_FILE = 7
        val MPV_EVENT_FILE_LOADED = 8
        val MPV_EVENT_TRACKS_CHANGED = 9
        val MPV_EVENT_TRACK_SWITCHED = 10
        val MPV_EVENT_IDLE = 11
        val MPV_EVENT_PAUSE = 12
        val MPV_EVENT_UNPAUSE = 13
        val MPV_EVENT_TICK = 14
        val MPV_EVENT_SCRIPT_INPUT_DISPATCH = 15
        val MPV_EVENT_CLIENT_MESSAGE = 16
        val MPV_EVENT_VIDEO_RECONFIG = 17
        val MPV_EVENT_AUDIO_RECONFIG = 18
        val MPV_EVENT_METADATA_UPDATE = 19
        val MPV_EVENT_SEEK = 20
        val MPV_EVENT_PLAYBACK_RESTART = 21
        val MPV_EVENT_PROPERTY_CHANGE = 22
        val MPV_EVENT_CHAPTER_CHANGE = 23
        val MPV_EVENT_QUEUE_OVERFLOW = 24
    }
}
