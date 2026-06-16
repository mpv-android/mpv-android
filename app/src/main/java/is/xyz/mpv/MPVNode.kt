package `is`.xyz.mpv

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class MPVNode {
    object None : MPVNode()
    data class StringNode(val value: String) : MPVNode()
    data class BooleanNode(val value: Boolean) : MPVNode()
    data class IntNode(val value: Long) : MPVNode()
    data class DoubleNode(val value: Double) : MPVNode()
    data class ByteArrayNode(val value: ByteArray) : MPVNode()
    data class ArrayNode(val value: Array<MPVNode>) : MPVNode()
    data class MapNode(val value: Map<String, MPVNode>) : MPVNode()

    @OptIn(ExperimentalEncodingApi::class)
    fun toJson(): String {
        return when (this) {
            is None -> "null"
            is StringNode -> "\"${value.replace("\"", "\\\"")}\""
            is BooleanNode -> if (value) "true" else "false"
            is IntNode -> value.toString()
            is DoubleNode -> value.toString()
            is ByteArrayNode -> "\"${Base64.encode(value)}\""
            is ArrayNode -> "[${value.joinToString(",") { it.toJson() }}]"
            is MapNode -> "{${value.entries.joinToString(",") { "\"${it.key}\":${it.value.toJson()}" }}}"
        }
    }

    fun asString(): String? = (this as? StringNode)?.value
    fun asBoolean(): Boolean? = (this as? BooleanNode)?.value
    fun asInt(): Long? = (this as? IntNode)?.value
    fun asDouble(): Double? = (this as? DoubleNode)?.value
    fun asByteArray(): ByteArray? = (this as? ByteArrayNode)?.value
    fun asArray(): Array<MPVNode>? = (this as? ArrayNode)?.value
    fun asMap(): Map<String, MPVNode>? = (this as? MapNode)?.value

    fun keys(): Set<String> = asMap()?.keys ?: emptySet()

    fun size(): Int = when (this) {
        is ArrayNode -> value.size
        is MapNode -> value.size
        else -> 0
    }

    operator fun get(index: Int): MPVNode? = asArray()?.getOrNull(index)
    operator fun get(key: String): MPVNode? = asMap()?.get(key)

    fun isEmpty(): Boolean = when (this) {
        is ArrayNode -> value.isEmpty()
        is MapNode -> value.isEmpty()
        is StringNode -> value.isEmpty()
        is None -> true
        else -> false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MPVNode) return false

        return when {
            this is ArrayNode && other is ArrayNode -> value.contentEquals(other.value)
            this is ByteArrayNode && other is ByteArrayNode -> value.contentEquals(other.value)
            else -> when (this) {
                is None -> other is None
                is StringNode -> other is StringNode && value == other.value
                is BooleanNode -> other is BooleanNode && value == other.value
                is IntNode -> other is IntNode && value == other.value
                is DoubleNode -> other is DoubleNode && value == other.value
                is MapNode -> other is MapNode && value == other.value
                else -> false
            }
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is None -> 0
            is StringNode -> value.hashCode()
            is BooleanNode -> value.hashCode()
            is IntNode -> value.hashCode()
            is DoubleNode -> value.hashCode()
            is ByteArrayNode -> value.contentHashCode()
            is ArrayNode -> value.contentHashCode()
            is MapNode -> value.hashCode()
        }
    }
}