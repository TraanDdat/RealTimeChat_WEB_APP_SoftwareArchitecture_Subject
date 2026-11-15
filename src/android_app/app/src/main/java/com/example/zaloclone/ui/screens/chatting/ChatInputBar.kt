package com.example.zaloclone.ui.screens.chatting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleAttach: () -> Unit,
    onToggleEmotion: () -> Unit,
    onFocusInput: () -> Unit,
    showAttachPanel: Boolean,
    showEmotionSheet: Boolean
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleEmotion) {
                Icon(
                    imageVector = Icons.Outlined.Face,
                    contentDescription = "Emotion",
                    tint = if (showEmotionSheet) Color(0xFF5B61F4) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onToggleAttach) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Attach Options",
                    tint = if (showAttachPanel) Color(0xFF5B61F4) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = "Nhập tin nhắn...",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                },
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                singleLine = false,
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F2F7),
                    unfocusedContainerColor = Color(0xFFF2F2F7),
                    disabledContainerColor = Color(0xFFF2F2F7),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp, max = 120.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocusInput()
                        }
                    }
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5B61F4))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}