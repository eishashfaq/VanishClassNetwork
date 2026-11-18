
package com.example.fieldsync.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CONNECTION_MAX_NAME_LENGTH = 24

// App colour palette
private val VcnDark = Color(0xFF392653)
private val VcnText = Color(0xFF392653)
private val VcnGrey = Color(0xFF887B8C)
private val VcnBorder = Color(0xFFAE91A9)
private val VcnBackground = Color(0xFFFFF9F0)
private val VcnFieldBackground = Color(0xFFFFFCF7)
private val VcnAccent = Color(0xFFF4D5C2)

data class ConnectionDetails(
    val displayName: String,
    val relayAddress: String,
    val classCode: String
)

@Composable
fun VcnLogo(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.025f

        val tail = Path().apply {
            moveTo(w * 0.28f, h * 0.68f)
            quadraticTo(
                w * 0.25f, h * 0.83f,
                w * 0.13f, h * 0.91f
            )
            quadraticTo(
                w * 0.38f, h * 0.88f,
                w * 0.43f, h * 0.72f
            )
            close()
        }

        drawPath(
            path = tail,
            color = VcnDark
        )

        drawCircle(
            color = VcnDark,
            radius = w * 0.40f,
            center = Offset(w * 0.50f, h * 0.43f)
        )

        val center = Offset(w * 0.50f, h * 0.43f)
        val radius = w * 0.215f

        drawCircle(
            color = VcnBackground,
            radius = radius,
            center = center
        )

        drawCircle(
            color = VcnDark,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        drawLine(
            color = VcnDark,
            start = center,
            end = Offset(
                center.x,
                center.y - radius * 0.57f
            ),
            strokeWidth = stroke * 1.7f
        )

        drawLine(
            color = VcnDark,
            start = center,
            end = Offset(
                center.x + radius * 0.43f,
                center.y + radius * 0.25f
            ),
            strokeWidth = stroke * 1.7f
        )

        drawCircle(
            color = VcnDark,
            radius = stroke * 1.8f,
            center = center
        )
    }
}

@Composable
fun ConnectionSetupScreen(
    onConnect: (ConnectionDetails) -> Unit,
    initialDisplayName: String = "",
    initialRelayAddress: String = "",
    initialClassCode: String = ""
) {
    var displayName by remember(initialDisplayName) {
        mutableStateOf(initialDisplayName)
    }

    var relayAddress by remember(initialRelayAddress) {
        mutableStateOf(initialRelayAddress)
    }

    var classCode by remember(initialClassCode) {
        mutableStateOf(initialClassCode)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val cleanRelay = relayAddress.trim()

    val validRelay = cleanRelay.startsWith("wss://") ||
        cleanRelay.startsWith("ws://")

    val canConnect = displayName.isNotBlank() &&
        displayName.trim().length <= CONNECTION_MAX_NAME_LENGTH &&
        validRelay

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .background(VcnBackground)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            VcnLogo(size = 112.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vanish Class Network",
                color = VcnText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "Messages that don't stay forever",
                color = VcnGrey,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Let's get you connected",
                    color = VcnText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "Enter your details to join your class network.",
                    color = VcnGrey,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            SetupInput(
                label = "Your name",
                value = displayName,
                placeholder = "Enter your display name",
                onValueChange = {
                    displayName = it
                    errorMessage = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupInput(
                label = "Relay address",
                value = relayAddress,
                placeholder = "Enter your WebSocket relay address",
                onValueChange = {
                    relayAddress = it
                    errorMessage = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupInput(
                label = "Class code",
                value = classCode,
                placeholder = "Enter your class code",
                onValueChange = {
                    classCode = it
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Leave the class code empty if your relay does not require one.",
                color = VcnGrey,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFB3261E),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    when {
                        displayName.isBlank() -> {
                            errorMessage = "Please enter your name."
                        }

                        displayName.trim().length >
                            CONNECTION_MAX_NAME_LENGTH -> {
                            errorMessage =
                                "Name must be 24 characters or fewer."
                        }

                        cleanRelay.isBlank() -> {
                            errorMessage =
                                "Please enter your relay address."
                        }

                        !validRelay -> {
                            errorMessage =
                                "Relay address must start with ws:// or wss://."
                        }

                        else -> {
                            errorMessage = ""

                            onConnect(
                                ConnectionDetails(
                                    displayName = displayName.trim(),
                                    relayAddress = cleanRelay,
                                    classCode = classCode.trim()
                                )
                            )
                        }
                    }
                },
                enabled = canConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VcnDark,
                    contentColor = Color.White,
                    disabledContainerColor = VcnBorder,
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "Connect to Class",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your class conversations disappear after 30 seconds.",
                color = VcnGrey,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SetupInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = VcnText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = VcnGrey,
                    fontSize = 13.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VcnDark,
                unfocusedBorderColor = VcnBorder,
                focusedTextColor = VcnText,
                unfocusedTextColor = VcnText,
                cursorColor = VcnDark,
                focusedContainerColor = VcnFieldBackground,
                unfocusedContainerColor = VcnFieldBackground
            )
        )
    }
}

