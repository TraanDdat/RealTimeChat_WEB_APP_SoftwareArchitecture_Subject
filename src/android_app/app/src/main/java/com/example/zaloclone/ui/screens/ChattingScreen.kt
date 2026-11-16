package com.example.zaloclone.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.zaloclone.api.model.ChatUser
import com.example.zaloclone.api.model.SendMessageRequest
import com.example.zaloclone.data.local.*
import com.example.zaloclone.ui.model.ChatItem
import com.example.zaloclone.ui.model.Message
import com.example.zaloclone.ui.model.MessageType
import com.example.zaloclone.ui.model.getPreviewTextForMessage
import com.example.zaloclone.ui.screens.chatting.*
import com.example.zaloclone.ui.viewModel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChattingScreen(
    chatId: String,
    chatName: String,
    chatViewModel: ChatViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    val chatState = chatViewModel?.state?.collectAsState()

    var chatItem by remember { mutableStateOf<ChatItem?>(null) }
    var currentChatName by remember { mutableStateOf(chatName) }

    LaunchedEffect(chatName) {
        if (chatName.isNotBlank()) {
            currentChatName = chatName
        }
    }
    var showChangeGroupNameDialog by remember { mutableStateOf(false) }
    var showIndividualSettingsDialog by remember { mutableStateOf(false) }
    val groupMembers = remember { mutableStateListOf<String>() }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    var showAttachPanel by remember { mutableStateOf(false) }
    var showEmotionSheet by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val isGroup = remember(chatId, chatItem) {
        chatId.startsWith("group_") || (chatItem?.isGroup == true)
    }

    LaunchedEffect(chatId) {
        val targetUserId = chatId.toIntOrNull()
        if (targetUserId != null && chatViewModel != null) {
            val targetUser = ChatUser(id = targetUserId, username = chatName, email = "", profilePic = null)
            chatViewModel.setSelectedUser(targetUser)
        } else {
            messages.clear()
            val savedMessages = getOrInitChatHistory(context, chatId, chatName)
            messages.addAll(savedMessages)
        }
    }

    LaunchedEffect(chatState?.value?.messages) {
        val dbMessages = chatState?.value?.messages
        if (dbMessages != null && chatViewModel != null) {
            val mappedMessages = dbMessages.map { chatMsg ->
                val currentUserId = chatViewModel?.getCurrentUserId()
                val isMe = (chatMsg.senderId == currentUserId)
                val timeStr = try {
                    if (chatMsg.createdAt.length >= 16) chatMsg.createdAt.substring(11, 16) else chatMsg.createdAt
                } catch (e: Exception) {
                    ""
                }
                Message(
                    id = chatMsg.id.toString(),
                    text = chatMsg.text ?: "",
                    isMe = isMe,
                    type = if (!chatMsg.image.isNullOrBlank()) MessageType.IMAGE else MessageType.TEXT,
                    timestamp = timeStr,
                    mediaUrl = chatMsg.image
                )
            }
            messages.clear()
            messages.addAll(mappedMessages)
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    fun sendMediaMessage(
        type: MessageType,
        text: String = "",
        mediaUrl: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        emotion: String? = null
    ) {
        if (chatViewModel != null && chatId.toIntOrNull() != null) {
            val sendReq = SendMessageRequest(
                text = text.ifBlank { null },
                image = mediaUrl
            )
            val currentUserId = chatViewModel?.getCurrentUserId()
            chatViewModel.sendMessage(sendReq, currentUserId!!.toInt())
        } else {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newMsg = Message(
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
            messages.add(newMsg)
            saveMessagesToJson(context, chatId, messages.toList())

            val chatList = loadChatListFromJson(context).toMutableList()
            val existingIndex = chatList.indexOfFirst { it.id == chatId }
            val lastPreviewText = getPreviewTextForMessage(newMsg)

            if (existingIndex != -1) {
                val updatedChat = chatList[existingIndex].copy(
                    lastMessage = lastPreviewText,
                    time = currentTime,
                    unreadCount = 0
                )
                chatList[existingIndex] = updatedChat
                saveChatListToJson(context, chatList)
            } else {
                val friendList = loadFriendListFromJson(context)
                val friend = friendList.find { it.id == chatId }
                val targetAvatarBg = friend?.safeAvatarBgColor ?: Color(0xFF38C7FF)
                val targetAvatarUri = friend?.avatarUri
                val targetName = if (friend != null && friend.nickname.isNotBlank()) friend.nickname else currentChatName

                val newChat = ChatItem(
                    id = chatId,
                    name = targetName,
                    lastMessage = lastPreviewText,
                    time = currentTime,
                    unreadCount = 0,
                    avatarBgColor = targetAvatarBg,
                    avatarUri = targetAvatarUri
                )
                chatList.add(0, newChat)
                saveChatListToJson(context, chatList)
            }

            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

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

    LaunchedEffect(chatId) {
        messages.clear()
        val savedMessages = getOrInitChatHistory(context, chatId, chatName)
        messages.addAll(savedMessages)

        if (chatName.isNotBlank()) {
            currentChatName = chatName
        }

        val chatList = loadChatListFromJson(context).toMutableList()
        val index = chatList.indexOfFirst { it.id == chatId }
        if (index != -1) {
            chatItem = chatList[index]
            if (chatName.isBlank()) {
                currentChatName = chatList[index].name
            }
            if (chatList[index].unreadCount > 0) {
                chatList[index] = chatList[index].copy(unreadCount = 0)
                saveChatListToJson(context, chatList)
            }
        } else if (!chatId.startsWith("group_")) {
            val friendList = loadFriendListFromJson(context)
            val friend = friendList.find { it.id == chatId }
            if (friend != null) {
                if (chatName.isBlank()) {
                    currentChatName = if (friend.nickname.isNotBlank()) friend.nickname else friend.name
                }
            } else {
                if (chatName.isNotBlank()) {
                    currentChatName = chatName
                }
            }
        } else {
            if (chatName.isNotBlank()) {
                currentChatName = chatName
            }
        }

        groupMembers.clear()
        val membersFromItem = chatItem?.safeMembers ?: emptyList()
        if (membersFromItem.isNotEmpty()) {
            groupMembers.addAll(membersFromItem)
        } else if (isGroup) {
            groupMembers.add("Me")
        }

        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ChatTopBar(
                name = currentChatName,
                status = if (isGroup) "${groupMembers.size} Members" else "Online",
                isGroup = isGroup,
                memberCount = groupMembers.size,
                onBackClick = onNavigateBack,
                onInfoClick = {
                    if (isGroup) {
                        showGroupInfoDialog = true
                    } else {
                        showIndividualSettingsDialog = true
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

    previewImageUrl?.let { url ->
        FullscreenImageViewerDialog(
            imageUrl = url,
            onDismiss = { previewImageUrl = null }
        )
    }

    if (showGroupInfoDialog && isGroup) {
        GroupInfoDialog(
            groupName = currentChatName,
            members = groupMembers,
            onDismiss = { showGroupInfoDialog = false },
            onAddMemberClick = {
                showAddMemberDialog = true
            },
            onChangeGroupNameClick = {
                showChangeGroupNameDialog = true
            }
        )
    }

    if (showChangeGroupNameDialog && isGroup) {
        ChangeGroupNameDialog(
            currentName = currentChatName,
            onDismiss = { showChangeGroupNameDialog = false },
            onSave = { newName ->
                showChangeGroupNameDialog = false
                currentChatName = newName
                updateGroupName(context, chatId, newName)
            }
        )
    }

    if (showIndividualSettingsDialog && !isGroup) {
        IndividualChatSettingsDialog(
            context = context,
            friendId = chatId,
            friendName = chatName,
            onDismiss = { showIndividualSettingsDialog = false },
            onNicknameUpdated = { newDisplayName ->
                showIndividualSettingsDialog = false
                currentChatName = newDisplayName
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
                val noticeText = "Added ${newNames.joinToString(", ")} to the group."
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