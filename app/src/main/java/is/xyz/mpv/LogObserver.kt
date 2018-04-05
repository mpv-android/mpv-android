package `is`.xyz.mpv

interface LogObserver {
    fun logMessage(prefix: String, level: Int, text: String)
}