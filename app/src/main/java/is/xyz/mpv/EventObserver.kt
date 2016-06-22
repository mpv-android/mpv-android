package `is`.xyz.mpv

interface EventObserver {
    fun eventProperty(property: String)
    fun eventProperty(property: String, value: Long)
    fun eventProperty(property: String, value: Boolean)
    fun eventProperty(property: String, value: String)
    fun event(eventId: Int)
}
