package com.example.fieldsync.chat

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class ChatSocketService(
    private val onConnected: (Int) -> Unit,
    private val onDisconnected: (String) -> Unit,
    private val onPacket: (WirePacket) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        const val RELAY_URL =
            "wss://vanish-backend-mcls.onrender.com"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var connected = false

    private var heartbeatRunnable: Runnable? = null

    @Synchronized
    fun connect(
        deviceId: String,
        displayName: String,
        roomCode: String = "",
        relayAddress: String = RELAY_URL
    ) {
        if (webSocket != null) return

        try {
            val request = Request.Builder()
                .url(relayAddress.trim())
                .build()

            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {

                    override fun onOpen(
                        socket: WebSocket,
                        response: Response
                    ) {
                        val payload = JoinPayload(
                            device_id = deviceId,
                            display_name = displayName,
                            room_code = roomCode.ifBlank { null }
                        )

                        socket.send(
                            ChatProtocol.makePacket("JOIN", payload)
                        )
                    }

                    override fun onMessage(
                        socket: WebSocket,
                        text: String
                    ) {
                        try {
                            val packet =
                                ChatProtocol.parsePacket(text)

                            when (packet.type) {
                                "JOIN_ACK" -> {
                                    val ack =
                                        ChatProtocol.parsePayload(
                                            packet.payload,
                                            JoinAckPayload::class.java
                                        )

                                    if (ack.ok) {
                                        connected = true
                                        startHeartbeat(socket)

                                        mainHandler.post {
                                            onConnected(ack.online_count)
                                        }
                                    } else {
                                        mainHandler.post {
                                            onError(
                                                ack.error ?: "Join rejected"
                                            )
                                        }
                                        close()
                                    }
                                }

                                "HEARTBEAT_ACK" -> {
                                    val ack =
                                        ChatProtocol.parsePayload(
                                            packet.payload,
                                            HeartbeatAckPayload::class.java
                                        )

                                    mainHandler.post {
                                        onConnected(ack.online_count)
                                    }
                                }

                                else -> {
                                    mainHandler.post {
                                        onPacket(packet)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            mainHandler.post {
                                onError(
                                    "Invalid server packet: ${e.message}"
                                )
                            }
                        }
                    }

                    override fun onMessage(
                        socket: WebSocket,
                        bytes: ByteString
                    ) {
                        mainHandler.post {
                            onError("Unexpected binary packet")
                        }
                    }

                    override fun onClosing(
                        socket: WebSocket,
                        code: Int,
                        reason: String
                    ) {
                        socket.close(code, reason)
                    }

                    override fun onClosed(
                        socket: WebSocket,
                        code: Int,
                        reason: String
                    ) {
                        connected = false
                        stopHeartbeat()

                        synchronized(this@ChatSocketService) {
                            if (webSocket === socket) {
                                webSocket = null
                            }
                        }

                        mainHandler.post {
                            onDisconnected(
                                reason.ifBlank { "Connection closed" }
                            )
                        }
                    }

                    override fun onFailure(
                        socket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        connected = false
                        stopHeartbeat()

                        synchronized(this@ChatSocketService) {
                            if (webSocket === socket) {
                                webSocket = null
                            }
                        }

                        mainHandler.post {
                            onDisconnected(
                                t.message ?: "Connection failed"
                            )
                        }
                    }
                }
            )
        } catch (e: Exception) {
            webSocket = null
            mainHandler.post {
                onError(e.message ?: "Invalid relay address")
            }
        }
    }

    fun sendPacket(
        type: String,
        payload: Any
    ): Boolean {
        if (!connected) return false

        val socket = webSocket ?: return false

        return socket.send(
            ChatProtocol.makePacket(type, payload)
        )
    }

    private fun startHeartbeat(socket: WebSocket) {
        stopHeartbeat()

        val task = object : Runnable {
            override fun run() {
                if (connected && webSocket === socket) {
                    val payload = HeartbeatPayload(
                        client_time = System.currentTimeMillis()
                    )

                    socket.send(
                        ChatProtocol.makePacket(
                            "HEARTBEAT",
                            payload
                        )
                    )

                    mainHandler.postDelayed(
                        this,
                        HEARTBEAT_INTERVAL_MS
                    )
                }
            }
        }

        heartbeatRunnable = task

        mainHandler.postDelayed(
            task,
            HEARTBEAT_INTERVAL_MS
        )
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        heartbeatRunnable = null
    }

    @Synchronized
    fun close() {
        connected = false
        stopHeartbeat()

        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    fun isConnected(): Boolean = connected
}
