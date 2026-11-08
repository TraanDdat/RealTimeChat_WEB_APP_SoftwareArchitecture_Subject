package com.example.zaloclone.ui.screens

import android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChattingScreen(
    chatId: String,
    chatName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }

    // State lưu danh sách tin nhắn, UI sẽ tự động update khi list này thay đổi
    val messages = remember { mutableStateListOf<Message>() }

    // Load dữ liệu từ file JSON theo chatId khi mở màn hình
    LaunchedEffect(chatId) {
        messages.clear()
        val savedMessages = getOrInitChatHistory(context, chatId, chatName)
        messages.addAll(savedMessages)

        // Reset unreadCount to 0 when chat is opened/clicked
        val chatList = loadChatListFromJson(context).toMutableList()
        val index = chatList.indexOfFirst { it.id == chatId }
        if (index != -1 && chatList[index].unreadCount > 0) {
            chatList[index] = chatList[index].copy(unreadCount = 0)
            saveChatListToJson(context, chatList)
        }

        // Tự động cuộn xuống dòng cuối
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ChatTopBar(
                name = chatName,
                status = "Online",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        // 1. Tạo và lưu tin nhắn vào Chat History cho chatId này
                        val newMessage = Message(
                            id = System.currentTimeMillis().toString(),
                            text = messageText,
                            isMe = true
                        )
                        messages.add(newMessage)
                        saveMessagesToJson(context, chatId, messages.toList())

                        // 2. Cập nhật last message & time vào Chat List JSON
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val chatList = loadChatListFromJson(context).toMutableList()

                        val existingIndex = chatList.indexOfFirst { it.id == chatId }

                        if (existingIndex != -1) {
                            // Đã có trong danh sách -> Cập nhật
                            val updatedChat = chatList[existingIndex].copy(
                                lastMessage = messageText,
                                time = currentTime,
                                unreadCount = 0
                            )
                            chatList[existingIndex] = updatedChat
                        } else {
                            // Chưa có -> Tạo mới
                            chatList.add(
                                ChatItem(id = chatId, name = chatName, lastMessage = messageText, time = currentTime, unreadCount = 0)
                            )
                        }
                        // Ghi lại danh sách cập nhật xuống file JSON
                        saveChatListToJson(context, chatList)

                        // 3. Xóa khung nhập và cuộn xuống
                        messageText = ""
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    if (message.timestamp != null) {
                        TimeDivider(time = message.timestamp)
                    } else {
                        MessageBubble(message = message, senderName = chatName)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CÁC COMPONENT GIAO DIỆN PHỤ
// ==========================================
@Composable
fun ChatTopBar(name: String, status: String, onBackClick: () -> Unit) {
    Surface(shadowElevation = 2.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = status, fontSize = 12.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color.Gray)
                }
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, senderName: String) {
    val bubbleColor = if (message.isMe) Color(0xFFF2F2F7) else Color.White
    val borderColor = if (message.isMe) Color.Transparent else Color(0xFFE5E5EA)
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isMe) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE2C4B1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(senderName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = bubbleColor,
                shape = shape,
                border = if (!message.isMe) BorderStroke(1.dp, borderColor) else null
            ) {
                if (message.type == MessageType.TEXT) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                } else if (message.type == MessageType.AUDIO) {
                    AudioMessageContent(duration = message.duration ?: "0:00")
                }
            }
        }
    }
}

@Composable
fun AudioMessageContent(duration: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).width(180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8E8F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF5B61F4), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE5E5EA))) {
                Box(modifier = Modifier.fillMaxWidth(0.3f).height(4.dp).background(Color(0xFF5B61F4)))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = duration, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TimeDivider(time: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text = time, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.Face, contentDescription = "Emoji", tint = Color.Gray, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text("Type a message here", color = Color.LightGray, fontSize = 15.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (text.isNotEmpty()) {
                Button(
                    onClick = onSend,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8E8F5)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Send", color = Color(0xFF5B61F4), fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_add),
                    contentDescription = "Attach",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
