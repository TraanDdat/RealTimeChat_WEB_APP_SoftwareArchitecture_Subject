package com.example.zaloclone.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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
    val messages = remember { mutableStateListOf<Message>() }

    var chatItem by remember { mutableStateOf<ChatItem?>(null) }
    val groupMembers = remember { mutableStateListOf<String>() }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    var showAttachPanel by remember { mutableStateOf(false) }
    var showEmotionSheet by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val isGroup = remember(chatId, chatItem) {
        chatId.startsWith("group_") || (chatItem?.isGroup == true)
    }

    // Helper hàm gửi tin nhắn đa phương tiện
    fun sendMediaMessage(
        type: MessageType,
        text: String = "",
        mediaUrl: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        emotion: String? = null
    ) {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newMessage = Message(
            id = System.currentTimeMillis().toString(),
            text = text,
            isMe = true,
            type = type,
            timestamp = currentTime,
            mediaUrl = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            emotion = emotion
        )
        messages.add(newMessage)
        saveMessagesToJson(context, chatId, messages.toList())

        val previewText = getPreviewTextForMessage(newMessage)
        val chatList = loadChatListFromJson(context).toMutableList()
        val existingIndex = chatList.indexOfFirst { it.id == chatId }

        if (existingIndex != -1) {
            val updatedChat = chatList[existingIndex].copy(
                lastMessage = previewText,
                time = currentTime,
                unreadCount = 0
            )
            chatList[existingIndex] = updatedChat
        } else {
            chatList.add(
                ChatItem(
                    id = chatId,
                    name = chatName,
                    lastMessage = previewText,
                    time = currentTime,
                    unreadCount = 0,
                    isGroup = isGroup,
                    members = groupMembers.toList()
                )
            )
        }
        saveChatListToJson(context, chatList)

        coroutineScope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Launchers cho việc chọn tệp phương tiện (Image, Video, Document)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sendMediaMessage(
                type = MessageType.IMAGE,
                mediaUrl = it.toString()
            )
            showAttachPanel = false
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sendMediaMessage(
                type = MessageType.VIDEO,
                mediaUrl = it.toString()
            )
            showAttachPanel = false
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fName, fSize) = getFileNameAndSizeFromUri(context, it)
            sendMediaMessage(
                type = MessageType.DOCUMENT,
                mediaUrl = it.toString(),
                fileName = fName,
                fileSize = fSize
            )
            showAttachPanel = false
        }
    }

    // Load dữ liệu từ file JSON theo chatId khi mở màn hình
    LaunchedEffect(chatId) {
        messages.clear()
        val savedMessages = getOrInitChatHistory(context, chatId, chatName)
        messages.addAll(savedMessages)

        val chatList = loadChatListFromJson(context).toMutableList()
        val index = chatList.indexOfFirst { it.id == chatId }
        if (index != -1) {
            chatItem = chatList[index]
            if (chatList[index].unreadCount > 0) {
                chatList[index] = chatList[index].copy(unreadCount = 0)
                saveChatListToJson(context, chatList)
            }
        }

        groupMembers.clear()
        val membersFromItem = chatItem?.safeMembers ?: emptyList()
        if (membersFromItem.isNotEmpty()) {
            groupMembers.addAll(membersFromItem)
        } else if (isGroup) {
            groupMembers.add("Bạn (Tôi)")
        }

        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ChatTopBar(
                name = chatName,
                status = if (isGroup) "${groupMembers.size} thành viên" else "Online",
                isGroup = isGroup,
                memberCount = groupMembers.size,
                onBackClick = onNavigateBack,
                onInfoClick = {
                    if (isGroup) {
                        showGroupInfoDialog = true
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (showAttachPanel) {
                    AttachmentOptionPanel(
                        onSelectImage = { imagePickerLauncher.launch("image/*") },
                        onSelectVideo = { videoPickerLauncher.launch("video/*") },
                        onSelectDocument = { documentPickerLauncher.launch("*/*") },
                        onSelectEmotion = {
                            showAttachPanel = false
                            showEmotionSheet = true
                        }
                    )
                }

                if (showEmotionSheet) {
                    EmotionPickerSheet(
                        onDismiss = { showEmotionSheet = false },
                        onEmotionSelected = { emotion ->
                            sendMediaMessage(
                                type = MessageType.EMOTION,
                                emotion = emotion
                            )
                            showEmotionSheet = false
                        }
                    )
                }

                ChatInputBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            sendMediaMessage(
                                type = MessageType.TEXT,
                                text = messageText
                            )
                            messageText = ""
                        }
                    },
                    onToggleAttach = {
                        showAttachPanel = !showAttachPanel
                        if (showAttachPanel) showEmotionSheet = false
                    },
                    onToggleEmotion = {
                        showEmotionSheet = !showEmotionSheet
                        if (showEmotionSheet) showAttachPanel = false
                    },
                    onFocusInput = {
                        showAttachPanel = false
                        showEmotionSheet = false
                    },
                    showAttachPanel = showAttachPanel,
                    showEmotionSheet = showEmotionSheet
                )
            }
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
                    if (message.timestamp != null && message.text.isEmpty() && message.mediaUrl == null && message.emotion == null) {
                        TimeDivider(time = message.timestamp)
                    } else {
                        MessageBubble(
                            message = message,
                            senderName = chatName,
                            onImageClick = { url -> previewImageUrl = url },
                            onReactionSelect = { msg, reaction ->
                                val index = messages.indexOfFirst { it.id == msg.id }
                                if (index != -1) {
                                    val updatedMsg = messages[index].copy(
                                        reaction = if (messages[index].reaction == reaction) null else reaction
                                    )
                                    messages[index] = updatedMsg
                                    saveMessagesToJson(context, chatId, messages.toList())
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog Xem Ảnh Phóng To
    previewImageUrl?.let { url ->
        FullscreenImageViewerDialog(
            imageUrl = url,
            onDismiss = { previewImageUrl = null }
        )
    }

    if (showGroupInfoDialog && isGroup) {
        GroupInfoDialog(
            groupName = chatName,
            members = groupMembers,
            onDismiss = { showGroupInfoDialog = false },
            onAddMemberClick = {
                showAddMemberDialog = true
            }
        )
    }

    if (showAddMemberDialog && isGroup) {
        AddMemberDialog(
            context = context,
            currentMembers = groupMembers,
            onDismiss = { showAddMemberDialog = false },
            onMembersAdded = { newNames ->
                showAddMemberDialog = false
                groupMembers.addAll(newNames)
                updateGroupMembers(context, chatId, groupMembers.toList())

                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val noticeText = "Đã thêm ${newNames.joinToString(", ")} vào nhóm"
                val noticeMsg = Message(
                    id = System.currentTimeMillis().toString(),
                    text = noticeText,
                    isMe = true
                )
                messages.add(noticeMsg)
                saveMessagesToJson(context, chatId, messages.toList())

                val chatList = loadChatListFromJson(context).toMutableList()
                val existingIndex = chatList.indexOfFirst { it.id == chatId }
                if (existingIndex != -1) {
                    val updatedChat = chatList[existingIndex].copy(
                        lastMessage = noticeText,
                        time = currentTime,
                        unreadCount = 0,
                        members = groupMembers.toList()
                    )
                    chatList[existingIndex] = updatedChat
                    saveChatListToJson(context, chatList)
                }
            }
        )
    }
}

// ==========================================
// 4. HÀM TRỢ GIÚP & CÁC COMPONENT GIAO DIỆN PHỤ
// ==========================================
fun getFileNameAndSizeFromUri(context: Context, uri: Uri): Pair<String, String> {
    var name = "Document"
    var sizeStr = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val foundName = cursor.getString(nameIndex)
                    if (!foundName.isNullOrEmpty()) name = foundName
                }
                if (sizeIndex != -1) {
                    val bytes = cursor.getLong(sizeIndex)
                    sizeStr = when {
                        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
                        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
                        else -> "$bytes B"
                    }
                }
            }
        }
    } catch (e: Exception) {
        name = uri.lastPathSegment ?: "Document"
    }
    return Pair(name, sizeStr)
}

@Composable
fun AttachmentOptionPanel(
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectDocument: () -> Unit,
    onSelectEmotion: () -> Unit
) {
    Surface(
        color = Color(0xFFF9F9FB),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttachmentOptionItem(
                icon = Icons.Default.Image,
                label = "Hình ảnh",
                bgColor = Color(0xFF4CAF50),
                onClick = onSelectImage
            )
            AttachmentOptionItem(
                icon = Icons.Default.Videocam,
                label = "Video",
                bgColor = Color(0xFFF44336),
                onClick = onSelectVideo
            )
            AttachmentOptionItem(
                icon = Icons.Default.InsertDriveFile,
                label = "Tài liệu",
                bgColor = Color(0xFF2196F3),
                onClick = onSelectDocument
            )
            AttachmentOptionItem(
                icon = Icons.Default.EmojiEmotions,
                label = "Cảm xúc",
                bgColor = Color(0xFFFF9800),
                onClick = onSelectEmotion
            )
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun EmotionPickerSheet(
    onDismiss: () -> Unit,
    onEmotionSelected: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val emojis = listOf(
        "😊", "😂", "❤️", "👍", "🔥", "🎉", "🥳", "😮",
        "😢", "😡", "👏", "💯", "💩", "🙏", "✨", "😍",
        "🤔", "😎", "🤣", "💖", "🥰", "🙌", "💥", "⭐",
        "🤩", "😴", "🤝", "😇", "🥳", "💪", "🚀", "🎯"
    )

    val stickers = listOf(
        "🐱 Mèo Vui Vẻ", "🐻 Gấu Thương Thương", "🐰 Thỏ Cười Khì", "🐶 Chó Tung Hoa",
        "🦊 Cáo Thả Tim", "🐼 Gấu Trúc Say Hi", "🦁 Sư Tử Cool", "🐸 Ếch Nhảy Múa",
        "🐣 Gà Con Xinh", "🐯 Hổ Dễ Thương", "🦄 Kỳ Lân May Mắn", "🐙 Bạch Tuộc Yêu"
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Biểu cảm", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Nhãn dán", fontWeight = FontWeight.Bold) }
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onEmotionSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(stickers) { sticker ->
                        Surface(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { onEmotionSelected(sticker) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF2F2F7),
                            border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sticker,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    senderName: String,
    onImageClick: (String) -> Unit = {},
    onReactionSelect: (Message, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showReactionMenu by remember { mutableStateOf(false) }

    val bubbleColor = if (message.isMe) Color(0xFFF2F2F7) else Color.White
    val borderColor = if (message.isMe) Color.Transparent else Color(0xFFE5E5EA)
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
        ) {
            // Thanh thả thả cảm xúc (Reaction Popup Menu)
            if (showReactionMenu) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reactions = listOf("❤️", "👍", "😂", "😮", "😢", "😡")
                        reactions.forEach { reaction ->
                            Text(
                                text = reaction,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onReactionSelect(message, reaction)
                                        showReactionMenu = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
            ) {
                if (!message.isMe) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2C4B1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(senderName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box {
                    Surface(
                        color = if (message.type == MessageType.EMOTION) Color.Transparent else bubbleColor,
                        shape = shape,
                        border = if (!message.isMe && message.type != MessageType.EMOTION) BorderStroke(1.dp, borderColor) else null,
                        modifier = Modifier.clickable {
                            showReactionMenu = !showReactionMenu
                        }
                    ) {
                        when (message.type) {
                            MessageType.TEXT -> {
                                Text(
                                    text = message.text,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                            MessageType.AUDIO -> {
                                AudioMessageContent(duration = message.duration ?: "0:00")
                            }
                            MessageType.IMAGE -> {
                                ImageMessageContent(
                                    imageUrl = message.mediaUrl,
                                    onImageClick = { url -> onImageClick(url) }
                                )
                            }
                            MessageType.VIDEO -> {
                                VideoMessageContent(
                                    videoUrl = message.mediaUrl,
                                    onPlayClick = { url ->
                                        url?.let {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(it), "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                            MessageType.DOCUMENT -> {
                                DocumentMessageContent(
                                    fileName = message.fileName ?: "Document",
                                    fileSize = message.fileSize ?: "File",
                                    fileUrl = message.mediaUrl,
                                    onOpenClick = { url ->
                                        url?.let {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(it), "*/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                            MessageType.EMOTION -> {
                                EmotionMessageContent(emotion = message.emotion ?: "😊")
                            }
                        }
                    }

                    // Hiển thị badge Reaction nếu có
                    message.reaction?.let { reactionText ->
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                            modifier = Modifier
                                .align(if (message.isMe) Alignment.BottomStart else Alignment.BottomEnd)
                                .offset(y = 8.dp, x = if (message.isMe) (-6).dp else 6.dp)
                        ) {
                            Text(
                                text = reactionText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageMessageContent(imageUrl: String?, onImageClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .sizeIn(maxWidth = 220.dp, maxHeight = 240.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { imageUrl?.let { onImageClick(it) } }
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Image message",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Image", tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoMessageContent(videoUrl: String?, onPlayClick: (String?) -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onPlayClick(videoUrl) },
        contentAlignment = Alignment.Center
    ) {
        if (!videoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = videoUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = Color(0xFF5B61F4),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun DocumentMessageContent(
    fileName: String,
    fileSize: String,
    fileUrl: String?,
    onOpenClick: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .width(240.dp)
            .clickable { onOpenClick(fileUrl) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = "Document",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (fileSize.isNotBlank()) fileSize else "Tài liệu",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Open Document",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EmotionMessageContent(emotion: String) {
    Box(
        modifier = Modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (emotion.contains(" ")) {
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFE0B2))
            ) {
                Text(
                    text = emotion,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        } else {
            Text(
                text = emotion,
                fontSize = 44.sp
            )
        }
    }
}

@Composable
fun FullscreenImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ChatTopBar(
    name: String,
    status: String,
    isGroup: Boolean = false,
    memberCount: Int = 0,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = isGroup) { onInfoClick() }
            ) {
                Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isGroup) "$memberCount thành viên" else status,
                        fontSize = 12.sp,
                        color = if (isGroup) Color(0xFF5B61F4) else Color.Gray,
                        fontWeight = if (isGroup) FontWeight.Medium else FontWeight.Normal
                    )
                    if (isGroup) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Xem)",
                            fontSize = 11.sp,
                            color = Color(0xFF5B61F4)
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color.Gray)
                }
                IconButton(onClick = { onInfoClick() }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Group Info",
                        tint = if (isGroup) Color(0xFF5B61F4) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun GroupInfoDialog(
    groupName: String,
    members: List<String>,
    onDismiss: () -> Unit,
    onAddMemberClick: () -> Unit
) {
    val colors = listOf(
        Color(0xFF38C7FF), Color(0xFF5B61F4), Color(0xFF34C759),
        Color(0xFFFF9500), Color(0xFFAF52DE), Color(0xFFE2C4B1)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5B61F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = groupName.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = groupName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "${members.size} thành viên",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách thành viên (${members.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onAddMemberClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+ Thêm",
                            color = Color(0xFF5B61F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(members.size) { index ->
                        val memberName = members[index]
                        val memberColor = colors[memberName.hashCode().let { if (it < 0) -it else it } % colors.size]
                        val isLeader = index == 0 || memberName.contains("Bạn")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(memberColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = memberName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = memberName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isLeader) {
                                Surface(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Trưởng nhóm",
                                        color = Color(0xFF0288D1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Thành viên",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        if (index < members.size - 1) {
                            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B61F4))
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun AddMemberDialog(
    context: Context,
    currentMembers: List<String>,
    onDismiss: () -> Unit,
    onMembersAdded: (List<String>) -> Unit
) {
    val allFriends = remember { loadFriendListFromJson(context) }
    val availableFriends = remember(allFriends, currentMembers) {
        allFriends.filter { friend -> currentMembers.none { m -> m.contains(friend.name) || friend.name.contains(m) } }
    }
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thành viên vào nhóm", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (availableFriends.isEmpty()) {
                    Text(
                        text = "Tất cả bạn bè đã có trong nhóm.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(availableFriends) { friend ->
                            val isChecked = selected[friend.id] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected[friend.id] = !isChecked }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selected[friend.id] = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(friend.avatarBgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friend.name.take(1),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = friend.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newNames = availableFriends.filter { selected[it.id] == true }.map { it.name }
                    if (newNames.isNotEmpty()) {
                        onMembersAdded(newNames)
                    }
                },
                enabled = selected.any { it.value }
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun AudioMessageContent(duration: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E8F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF5B61F4),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E5EA))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(4.dp)
                        .background(Color(0xFF5B61F4))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = duration, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TimeDivider(time: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = time, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

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
