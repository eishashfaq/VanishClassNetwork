
package com.example.fieldsync.chat

import com.google.gson.Gson
import com.google.gson.JsonObject

const val PROTOCOL_VERSION = "1.0"
const val MESSAGE_LIFETIME_MS = 30_000L
const val MAX_QUEUE_AGE_MS = 60_000L
const val MAX_TEXT_LENGTH = 280
const val MAX_NAME_LENGTH = 24
const val HEARTBEAT_INTERVAL_MS = 15_000L

enum class MessageStatus {
    CREATED,
    WAITING,
    SENT,
    DELIVERED,
    DISPLAYED,
    EXPIRED,
    DROPPED
}

data class ChatMessage(
    val id: String,
    val sender: String,
    val device_id: String,
    val text: String,
    val sent_at: Long?,
    val expires_at: Long?,
    val status: MessageStatus,
    val created_at: Long? = null
)

data class WirePacket(
    val version: String,
    val type: String,
    val payload: JsonObject
)

data class JoinPayload(
    val device_id: String,
    val display_name: String,
    val room_code: String? = null
)

data class JoinAckPayload(
    val ok: Boolean,
    val server_time: Long,
    val online_count: Int,
    val error: String? = null
)

data class MessageSendPayload(
    val id: String,
    val sender: String,
    val device_id: String,
    val text: String,
    val queued_ms: Long
)

data class MessageDeliverPayload(
    val id: String,
    val sender: String,
    val device_id: String,
    val text: String,
    val sent_at: Long,
    val expires_at: Long
)

data class DeliveryAckPayload(
    val message_id: String,
    val status: String,
    val sent_at: Long?,
    val expires_at: Long?,
    val reason: String? = null
)

data class MessageExpiredPayload(
    val message_id: String
)

data class HeartbeatPayload(
    val client_time: Long
)

data class HeartbeatAckPayload(
    val server_time: Long,
    val online_count: Int
)

data class DisconnectPayload(
    val reason: String? = null
)

object ChatProtocol {
    private val gson = Gson()

    fun makePacket(type: String, payload: Any): String {
        val packet = WirePacket(
            version = PROTOCOL_VERSION,
            type = type,
            payload = gson.toJsonTree(payload).asJsonObject
        )
        return gson.toJson(packet)
    }

    fun parsePacket(json: String): WirePacket {
        return gson.fromJson(json, WirePacket::class.java)
    }

    fun <T> parsePayload(payload: JsonObject, type: Class<T>): T {
        return gson.fromJson(payload, type)
    }
}
