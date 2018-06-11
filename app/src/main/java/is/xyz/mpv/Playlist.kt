package `is`.xyz.mpv

interface PlaylistHandler {
    fun onNowPlaying(prettyName: String)
    fun onPlaylistOver()
}

private fun baseName(s: String): String {
    return s.substring(s.lastIndexOf('/') + 1)
}

internal class Playlist(private val player: MPVView, private val handler: PlaylistHandler): EventObserver {

    var list = arrayOf<String>()
    var pos = 0

    // Set to true when playback start event is received
    private var started = false

    init {
        player.addObserver(this)
    }

    fun playAt(at: Int): Boolean {
        if (at < 0 || at >= list.size)
            return false

        pos = at
        handler.onNowPlaying(baseName(list[pos]))
        player.playFile(list[pos])

        return true
    }

    fun playNext(): Boolean {
        return playAt(pos + 1)
    }

    fun playPrev(): Boolean {
        return playAt(pos - 1)
    }

    fun prettyEntries(): List<String> {
        return list.mapIndexed { idx, it ->
            var text = baseName(it)
            if (idx == pos)
                text = "▶ $text"
            text
        }
    }

    override fun eventProperty(property: String) {}
    override fun eventProperty(property: String, value: Long) {}
    override fun eventProperty(property: String, value: Boolean) {}
    override fun eventProperty(property: String, value: String) {}

    override fun event(eventId: Int) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_START_FILE)
            started = true

        if (started && eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE) {
            started = false
            // The file's over, so we need to play next entry in the playlist
            if (!playNext()) {
                // If there's nothing to play, notify our handler
                handler.onPlaylistOver()
            }
        }
    }
}
