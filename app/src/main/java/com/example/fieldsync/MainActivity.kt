
package com.example.fieldsync

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldsync.chat.ChatMessage
import com.example.fieldsync.chat.ChatScreen
import com.example.fieldsync.chat.ChatSocketService
import com.example.fieldsync.chat.ChatProtocol
import com.example.fieldsync.chat.ConnectionDetails
import com.example.fieldsync.chat.ConnectionSetupScreen
import com.example.fieldsync.chat.DeliveryAckPayload
import com.example.fieldsync.chat.MessageDeliverPayload
import com.example.fieldsync.chat.MessageExpiredPayload
import com.example.fieldsync.chat.MessageSendPayload
import com.example.fieldsync.chat.MessageStatus
import com.example.fieldsync.chat.VcnLogo
import com.example.fieldsync.chat.WirePacket
import com.example.fieldsync.chat.MAX_QUEUE_AGE_MS
import com.example.fieldsync.data.ChatMessageEntity
import com.example.fieldsync.data.DatabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(
            "vcn_preferences",
            MODE_PRIVATE
        )

        val deviceId = preferences.getString("device_id", null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit()
                    .putString("device_id", it)
                    .apply()
            }

        setContent {
            MaterialTheme {
                VcnApplication(
                    deviceId = deviceId,
                    preferences = preferences
                )
            }
        }
    }
}

@Composable
private fun VcnApplication(
    deviceId: String,
    preferences: SharedPreferences
) {
    val context = LocalContext.current

    val database = remember {
        DatabaseProvider.getDatabase(context)
    }

    val messageDao = database.chatMessageDao()
    val scope = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf("splash")
    }

    var displayName by remember {
        mutableStateOf(
            preferences.getString("display_name", "") ?: ""
        )
    }

    var classCode by remember {
        mutableStateOf(
            preferences.getString("class_code", "") ?: ""
        )
    }

    var relayAddress by remember {
        mutableStateOf(
            preferences.getString("relay_address", "") ?: ""
        )
    }

    var onlineCount by remember { mutableStateOf(0) }
    var isConnected by remember { mutableStateOf(false) }

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }

    var connectionError by remember {
        mutableStateOf("")
    }

    // Convert a database record into a chat message.
    fun toChatMessage(entity: ChatMessageEntity): ChatMessage {
        val status = runCatching {
            MessageStatus.valueOf(entity.status)
        }.getOrDefault(MessageStatus.WAITING)

        return ChatMessage(
            id = entity.id,
            sender = entity.sender,
            device_id = entity.deviceId,
            text = entity.text,
            sent_at = entity.sentAt,
            expires_at = entity.expiresAt,
            status = status,
            created_at = entity.createdAtMs
        )
    }

    // Convert a chat message into a database record.
    fun toEntity(
        message: ChatMessage,
        createdAtMs: Long
    ): ChatMessageEntity {
        return ChatMessageEntity(
            id = message.id,
            sender = message.sender,
            deviceId = message.device_id,
            text = message.text,
            sentAt = message.sent_at,
            expiresAt = message.expires_at,
            status = message.status.name,
            createdAtMs = createdAtMs
        )
    }

    // Mark an overdue pending message as dropped
    // and remove it from the active chat and database.
    suspend fun dropPendingMessage(messageId: String) {
        messageDao.updateStatus(
            messageId,
            "DROPPED"
        )

        messages = messages.filterNot {
            it.id == messageId
        }

        messageDao.deleteById(messageId)
    }

    // Handle packets received from the relay.
    val handlePacket: (WirePacket) -> Unit = { packet ->

        when (packet.type) {

            "MESSAGE_DELIVER" -> {
                try {
                    val data = ChatProtocol.parsePayload(
                        packet.payload,
                        MessageDeliverPayload::class.java
                    )

                    val receivedMessage = ChatMessage(
                        id = data.id,
                        sender = data.sender,
                        device_id = data.device_id,
                        text = data.text,
                        sent_at = data.sent_at,
                        expires_at = data.expires_at,
                        status = MessageStatus.DISPLAYED,
                        created_at = data.sent_at
                    )

                    if (messages.none {
                            it.id == receivedMessage.id
                        }
                    ) {
                        messages = messages + receivedMessage

                        scope.launch {
                            messageDao.upsert(
                                toEntity(
                                    receivedMessage,
                                    System.currentTimeMillis()
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    connectionError =
                        "Could not read received message"
                }
            }

            "DELIVERY_ACK" -> {
                try {
                    val ack = ChatProtocol.parsePayload(
                        packet.payload,
                        DeliveryAckPayload::class.java
                    )

                    if (ack.status == "DROPPED") {

                        messages = messages.filterNot {
                            it.id == ack.message_id
                        }

                        scope.launch {
                            messageDao.updateDelivery(
                                messageId = ack.message_id,
                                status = "DROPPED",
                                sentAt = ack.sent_at,
                                expiresAt = ack.expires_at
                            )

                            messageDao.deleteById(
                                ack.message_id
                            )
                        }

                    } else {

                        messages = messages.map { message ->
                            if (message.id == ack.message_id) {
                                message.copy(
                                    sent_at = ack.sent_at,
                                    expires_at = ack.expires_at,
                                    status = when (ack.status) {
                                        "DELIVERED" ->
                                            MessageStatus.DELIVERED

                                        else -> message.status
                                    }
                                )
                            } else {
                                message
                            }
                        }

                        scope.launch {
                            messageDao.updateDelivery(
                                messageId = ack.message_id,
                                status = when (ack.status) {
                                    "DELIVERED" -> "DELIVERED"
                                    else -> messages.firstOrNull {
                                        it.id == ack.message_id
                                    }?.status?.name ?: "WAITING"
                                },
                                sentAt = ack.sent_at,
                                expiresAt = ack.expires_at
                            )
                        }
                    }

                } catch (e: Exception) {
                    connectionError =
                        "Could not read delivery confirmation"
                }
            }

            "MESSAGE_EXPIRED" -> {
                try {
                    val expired = ChatProtocol.parsePayload(
                        packet.payload,
                        MessageExpiredPayload::class.java
                    )

                    messages = messages.filterNot {
                        it.id == expired.message_id
                    }

                    scope.launch {
                        messageDao.deleteById(
                            expired.message_id
                        )
                    }

                } catch (e: Exception) {
                    connectionError =
                        "Could not read expiry notice"
                }
            }
        }
    }

    val latestPacketHandler by rememberUpdatedState(
        handlePacket
    )

    val socketService = remember {
        ChatSocketService(
            onConnected = { count ->
                isConnected = true
                onlineCount = count
                connectionError = ""
            },

            onDisconnected = { reason ->
                isConnected = false
                onlineCount = 0
                connectionError = reason
            },

            onPacket = { packet ->
                latestPacketHandler(packet)
            },

            onError = { error ->
                connectionError = error
            }
        )
    }

    // Load messages from the database when entering chat.
    LaunchedEffect(currentScreen) {
        if (currentScreen == "chat") {
            val now = System.currentTimeMillis()

            messageDao.deleteExpired(now)

            val savedMessages =
                messageDao.getActiveMessages(now)

            messages = savedMessages.map {
                toChatMessage(it)
            }
        }
    }

    // Remove messages whose relay expiry timestamp has passed.
    LaunchedEffect(currentScreen) {
        if (currentScreen == "chat") {
            while (true) {
                delay(500)

                val now = System.currentTimeMillis()

                messages = messages.filter { message ->
                    message.expires_at == null ||
                        message.expires_at > now
                }

                messageDao.deleteExpired(now)
            }
        }
    }

    // Check the 60-second offline queue limit continuously.
    LaunchedEffect(currentScreen) {
        if (currentScreen == "chat") {
            while (true) {
                delay(500)

                val pendingMessages =
                    messageDao.getPendingMessages()

                val now = System.currentTimeMillis()

                for (saved in pendingMessages) {
                    val queueAge = now - saved.createdAtMs

                    if (queueAge >= MAX_QUEUE_AGE_MS) {
                        dropPendingMessage(saved.id)
                    }
                }
            }
        }
    }

    // Automatically retry the connection after disconnection.
    LaunchedEffect(
        currentScreen,
        isConnected,
        relayAddress,
        displayName,
        classCode
    ) {
        if (currentScreen == "chat" && !isConnected) {
            while (!isConnected) {
                if (relayAddress.isNotBlank()) {
                    socketService.connect(
                        deviceId = deviceId,
                        displayName = displayName,
                        roomCode = classCode,
                        relayAddress = relayAddress
                    )
                }

                delay(4000)
            }
        }
    }

    // Retry pending messages after connection returns.
    LaunchedEffect(isConnected) {
        if (isConnected) {

            val pendingMessages =
                messageDao.getPendingMessages()

            for (saved in pendingMessages) {

                val now = System.currentTimeMillis()
                val queueAge = now - saved.createdAtMs

                if (queueAge >= MAX_QUEUE_AGE_MS) {
                    dropPendingMessage(saved.id)
                    continue
                }

                val payload = MessageSendPayload(
                    id = saved.id,
                    sender = saved.sender,
                    device_id = saved.deviceId,
                    text = saved.text,
                    queued_ms = queueAge.coerceAtLeast(0L)
                )

                val sent = socketService.sendPacket(
                    "MESSAGE_SEND",
                    payload
                )

                if (sent) {

                    messageDao.updateStatus(
                        saved.id,
                        "SENT"
                    )

                    messages = messages.map { message ->
                        if (message.id == saved.id) {
                            message.copy(
                                status = MessageStatus.SENT
                            )
                        } else {
                            message
                        }
                    }

                } else {
                    break
                }
            }
        }
    }

    // Splash screen delay.
    LaunchedEffect(Unit) {
        delay(1500)
        currentScreen = "setup"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFF9F0)
    ) {
        when (currentScreen) {

            "splash" -> {
                SplashScreen()
            }

            "setup" -> {
                ConnectionSetupScreen(
                    initialDisplayName = displayName,
                    initialRelayAddress = relayAddress,
                    initialClassCode = classCode,

                    onConnect = { details: ConnectionDetails ->

                        displayName = details.displayName
                        classCode = details.classCode
                        relayAddress = details.relayAddress

                        preferences.edit()
                            .putString("display_name", displayName)
                            .putString("class_code", classCode)
                            .putString("relay_address", relayAddress)
                            .apply()

                        messages = emptyList()
                        isConnected = false
                        onlineCount = 0
                        connectionError = ""

                        currentScreen = "chat"

                        socketService.connect(
                            deviceId = deviceId,
                            displayName = displayName,
                            roomCode = classCode,
                            relayAddress = relayAddress
                        )
                    }
                )
            }

            "chat" -> {
                ChatScreen(
                    displayName = displayName,
                    currentDeviceId = deviceId,
                    messages = messages,
                    onlineCount = onlineCount,
                    isConnected = isConnected,
                    classCode = classCode,
                    relayAddress = relayAddress,

                    onSendMessage = { text ->

                        val cleanText = text.trim()

                        if (
                            cleanText.isNotEmpty() &&
                            cleanText.length <= 280
                        ) {
                            val now = System.currentTimeMillis()
                            val messageId =
                                UUID.randomUUID().toString()

                            val newMessage = ChatMessage(
                                id = messageId,
                                sender = displayName,
                                device_id = deviceId,
                                text = cleanText,
                                sent_at = null,
                                expires_at = null,
                                status = MessageStatus.WAITING,
                                created_at = now
                            )

                            messages = messages + newMessage

                            // Save before attempting transmission.
                            scope.launch {
                                messageDao.upsert(
                                    toEntity(
                                        newMessage,
                                        now
                                    )
                                )

                                if (isConnected) {
                                    val payload = MessageSendPayload(
                                        id = messageId,
                                        sender = displayName,
                                        device_id = deviceId,
                                        text = cleanText,
                                        queued_ms = 0L
                                    )

                                    val sent =
                                        socketService.sendPacket(
                                            "MESSAGE_SEND",
                                            payload
                                        )

                                    if (sent) {
                                        messageDao.updateStatus(
                                            messageId,
                                            "SENT"
                                        )

                                        messages = messages.map {
                                            if (it.id == messageId) {
                                                it.copy(
                                                    status = MessageStatus.SENT
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },

                    onSettingsSave = { updatedDetails ->

                        displayName = updatedDetails.displayName
                        classCode = updatedDetails.classCode
                        relayAddress = updatedDetails.relayAddress

                        preferences.edit()
                            .putString("display_name", displayName)
                            .putString("class_code", classCode)
                            .putString("relay_address", relayAddress)
                            .apply()

                        isConnected = false
                        onlineCount = 0
                        connectionError = ""

                        socketService.close()

                        socketService.connect(
                            deviceId = deviceId,
                            displayName = displayName,
                            roomCode = classCode,
                            relayAddress = relayAddress
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9F0)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        VcnLogo(size = 124.dp)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Vanish Class Network",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF392653)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Messages that don't stay forever",
            fontSize = 14.sp,
            color = Color(0xFF887B8C)
        )
    }
}
