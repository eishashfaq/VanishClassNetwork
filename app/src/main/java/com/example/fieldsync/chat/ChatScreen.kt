
package com.example.fieldsync.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// App colour palette
private val DeepViolet = Color(0xFF392653)
private val DustyMauve = Color(0xFFAE91A9)
private val PalePeach = Color(0xFFF4D5C2)
private val IvoryWhite = Color(0xFFFFF9F0)
private val LightLavender = Color(0xFFF0EAF4)

private val SecondaryText = DeepViolet.copy(alpha = 0.70f)
private val ProgressTrack = IvoryWhite

@Composable
fun ChatScreen(
    displayName: String,
    currentDeviceId: String,
    messages: List<ChatMessage>,
    onlineCount: Int,
    isConnected: Boolean,
    onSendMessage: (String) -> Unit,
    classCode: String = "",
    relayAddress: String = "",
    onSettingsSave: (ConnectionDetails) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    var currentTime by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    val listState = rememberLazyListState()

    // Refresh countdown and check message expiry continuously.
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(150)
        }
    }

    // Hide waiting messages after their original 60-second queue limit.
    // Also hide messages that have been marked DROPPED or EXPIRED.
    val visibleMessages = messages.filter { message ->
        when (message.status) {
            MessageStatus.WAITING -> {
                message.created_at?.let { createdAt ->
                    currentTime < createdAt + MAX_QUEUE_AGE_MS
                } ?: true
            }

            MessageStatus.DROPPED,
            MessageStatus.EXPIRED -> false

            else -> true
        }
    }

    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.lastIndex + 1)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            color = IvoryWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(IvoryWhite)
            ) {

                // Chat header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepViolet)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = IvoryWhite,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        VcnLogo(
                            size = 40.dp,
                            modifier = Modifier.offset(y = 2.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "CS Chat Room",
                            color = IvoryWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            Color(0xFFB8E6C1),
                                            CircleShape
                                        )
                                )
                            }

                            Text(
                                text = if (isConnected) {
                                    "Connected · ${onlineCount.coerceAtLeast(0)} online"
                                } else {
                                    "Disconnected · Messages will wait"
                                },
                                color = IvoryWhite,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⋮",
                                    color = IvoryWhite,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DustyMauve.copy(alpha = 0.45f))

                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(IvoryWhite)
                        .padding(horizontal = 12.dp),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "expiry_notice") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Messages disappear after 30 seconds",
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    items(
                        items = visibleMessages,
                        key = { it.id }
                    ) { message ->
                        val isMine =
                            message.device_id == currentDeviceId

                        MessageBubble(
                            message = message,
                            isMine = isMine,
                            currentTime = currentTime
                        )
                    }
                }

                // Message input area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PalePeach)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            if (it.length <= MAX_TEXT_LENGTH) {
                                messageText = it
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                color = SecondaryText,
                                fontSize = 14.sp
                            )
                        },
                        shape = CircleShape,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepViolet,
                            unfocusedBorderColor = DustyMauve,
                            focusedContainerColor = IvoryWhite,
                            unfocusedContainerColor = IvoryWhite,
                            focusedTextColor = DeepViolet,
                            unfocusedTextColor = DeepViolet,
                            cursorColor = DeepViolet
                        )
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    // Send button also works while disconnected.
                    Button(
                        onClick = {
                            val text = messageText.trim()

                            if (text.isNotEmpty()) {
                                onSendMessage(text)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 5.dp,
                            pressedElevation = 1.dp,
                            disabledElevation = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepViolet,
                            contentColor = IvoryWhite,
                            disabledContainerColor = DustyMauve,
                            disabledContentColor = IvoryWhite
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WhatsAppSendIcon()
                        }
                    }
                }
            }
        }
    }

    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            displayName = displayName,
            relayAddress = relayAddress,
            onDismiss = { showSettings = false },
            onSave = { details ->
                onSettingsSave(details)
                showSettings = false
            },
            classCode = classCode
        )
    }
}

@Composable
private fun WhatsAppSendIcon() {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(27.dp)
    ) {
        val plane = Path().apply {
            moveTo(size.width * 0.05f, size.height * 0.45f)
            lineTo(size.width * 0.95f, size.height * 0.03f)
            lineTo(size.width * 0.68f, size.height * 0.96f)
            lineTo(size.width * 0.47f, size.height * 0.60f)
            lineTo(size.width * 0.34f, size.height * 0.77f)
            lineTo(size.width * 0.36f, size.height * 0.55f)
            close()
        }

        drawPath(
            path = plane,
            color = IvoryWhite
        )

        drawLine(
            color = DeepViolet,
            start = androidx.compose.ui.geometry.Offset(
                size.width * 0.36f,
                size.height * 0.55f
            ),
            end = androidx.compose.ui.geometry.Offset(
                size.width * 0.95f,
                size.height * 0.03f
            ),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

@Composable
private fun SettingsDialog(
    displayName: String,
    relayAddress: String,
    onDismiss: () -> Unit,
    onSave: (ConnectionDetails) -> Unit,
    classCode: String
) {
    var editedName by remember(displayName) {
        mutableStateOf(displayName)
    }

    var editedRelayAddress by remember(relayAddress) {
        mutableStateOf(relayAddress)
    }

    val validRelay = editedRelayAddress.trim().let {
        it.startsWith("wss://") || it.startsWith("ws://")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                color = DeepViolet,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Update your username and connection address.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = editedName,
                    onValueChange = {
                        if (it.length <= MAX_NAME_LENGTH) {
                            editedName = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepViolet,
                        unfocusedBorderColor = DustyMauve,
                        focusedTextColor = DeepViolet,
                        unfocusedTextColor = DeepViolet,
                        cursorColor = DeepViolet
                    )
                )

                OutlinedTextField(
                    value = editedRelayAddress,
                    onValueChange = { editedRelayAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Relay address") },
                    placeholder = { Text("wss://your-relay-address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepViolet,
                        unfocusedBorderColor = DustyMauve,
                        focusedTextColor = DeepViolet,
                        unfocusedTextColor = DeepViolet,
                        cursorColor = DeepViolet
                    )
                )

                if (editedRelayAddress.isNotBlank() && !validRelay) {
                    Text(
                        text = "Address must start with ws:// or wss://",
                        color = Color(0xFFB3261E),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ConnectionDetails(
                            displayName = editedName.trim(),
                            relayAddress = editedRelayAddress.trim(),
                            classCode = classCode
                        )
                    )
                },
                enabled = editedName.isNotBlank() &&
                    editedRelayAddress.isNotBlank() &&
                    validRelay
            ) {
                Text(
                    text = "Save",
                    color = DeepViolet
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = DustyMauve
                )
            }
        },
        containerColor = IvoryWhite
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    currentTime: Long
) {
    val bubbleColor = if (isMine) {
        PalePeach
    } else {
        DustyMauve
    }

    val textColor = DeepViolet
    val secondaryColor = DeepViolet.copy(alpha = 0.75f)

    // Existing 30-second message expiry countdown.
    val remainingMs = message.expires_at?.let {
        (it - currentTime).coerceAtLeast(0L)
    }

    val remainingSeconds = remainingMs?.let {
        (it + 999L) / 1000L
    }

    val progress = remainingMs?.let {
        (it.toFloat() / MESSAGE_LIFETIME_MS.toFloat())
            .coerceIn(0f, 1f)
    }

    // 60-second countdown for waiting messages only.
    val waitingRemainingMs =
        if (message.status == MessageStatus.WAITING) {
            message.created_at?.let {
                (it + MAX_QUEUE_AGE_MS - currentTime)
                    .coerceAtLeast(0L)
            }
        } else {
            null
        }

    val waitingRemainingSeconds = waitingRemainingMs?.let {
        (it + 999L) / 1000L
    }

    val messageTime = message.sent_at?.let {
        SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        ).format(Date(it))
    } ?: ""

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val bubbleMaximumWidth = maxWidth * 0.55f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
        ) {
            Column(
                modifier = Modifier.widthIn(max = bubbleMaximumWidth),
                horizontalAlignment = if (isMine) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            color = bubbleColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(
                            horizontal = 13.dp,
                            vertical = 9.dp
                        )
                ) {
                    if (!isMine && message.sender.isNotBlank()) {
                        Text(
                            text = message.sender,
                            color = DeepViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    if (messageTime.isNotBlank()) {
                        Text(
                            text = messageTime,
                            color = secondaryColor,
                            fontSize = 10.sp
                        )
                    }

                    // Show waiting indicator only while waiting.
                    if (
                        message.status == MessageStatus.WAITING &&
                        waitingRemainingSeconds != null
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Waiting for connection · ${waitingRemainingSeconds}s left",
                            color = secondaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Existing 30-second expiry progress bar.
                    if (progress != null && remainingSeconds != null) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(ProgressTrack)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(DeepViolet)
                                )
                            }

                            Text(
                                text = "${remainingSeconds}s",
                                color = secondaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
