package `is`.xyz.mpv

/**
 * Compatibility event payload used by the mpvEx-derived Android AAR API.
 *
 * The current upstream JNI bridge does not expose node-valued event data, and
 * PlayBridge does not inspect the payload. Keeping the type and [None] value
 * lets existing consumers retain their event callback while the fork keeps
 * upstream's native event path unchanged.
 */
sealed class MPVNode {
    data object None : MPVNode()
}
