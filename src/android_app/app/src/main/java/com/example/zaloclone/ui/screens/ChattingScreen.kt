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
import com.example.zaloclone.ui.viewModel.ChatState

@Composable
fun ChattingScreen(
    chatId: String,
    chatName: String,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    val chatState by chatViewModel.state.collectAsState()
    val currentUserId = chatViewModel.getCurrentUserId()?: return

    val reactionByMessageId = remember {
        mutableStateMapOf<String, String>()
    }

    var currentChatName by remember { mutableStateOf(chatName) }

    var showChangeGroupNameDialog by remember { mutableStateOf(false) }
    var showIndividualSettingsDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    var showAttachPanel by remember { mutableStateOf(false) }
    var showEmotionSheet by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val isGroup = remember(chatId) {
        chatId.startsWith("group_")
    }

    val groupMembers = remember {
        mutableStateListOf<String>()
    }

    LaunchedEffect(chatName) {
        if (chatName.isNotBlank()) {
            currentChatName = chatName
        }
    }

    Text(
        text = """
            Chat ID: $chatId
            Is Group: $isGroup
            Selected User: ${chatState.selectedUser?.id}
            Selected Group: ${chatState.selectedGroup}
            Messages: ${chatState.messages.size}
            Error: ${chatState.errorMessage}
        """.trimIndent(),
        color = Color.Red
    )

    LaunchedEffect(chatId, chatViewModel) {
        reactionByMessageId.clear()
        groupMembers.clear()

        if(isGroup) {
            val groupId = chatId.removePrefix("group_").toIntOrNull()
            chatViewModel.setSelectedGroup(groupId)
            groupMembers.add("Me")
        } else {
            val userId = chatId.toIntOrNull()
            if (userId != null) {
                val targetUser = ChatUser(
                    id = userId,
                    username = chatName,
                    email = "",
                    profilePic = null
                )
                chatViewModel.setSelectedUser(targetUser)
            }
        }
    }

    LaunchedEffect(chatState.messages) {
        chatState.messages.forEach {
            Log.d(
                "CHAT_DEBUG",
                "Message id=${it.id}, sender=${it.senderId}, text=${it.text}"
            )
        }
    val messages = remember(
        chatState.messages,
        currentUserId,
        reactionByMessageId.toMap()
    ) {
        chatState.messages.map { chatMsg ->
            val isMe = chatMsg.senderId == currentUserId
            Message(
                id = chatMsg.id,
                text = chatMsg.text ?: "",
                isMe = isMe,
                type = if (!chatMsg.image.isNullOrBlank()) {
                    MessageType.IMAGE
                } else {
                    MessageType.TEXT
                },
                timestamp = chatMsg.createdAt,
                mediaUrl = chatMsg.image,
                reaction = reactionByMessageId[chatMsg.id]
            )
        }
    }

    LaunchedEffect(chatId, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
        val sendReq = SendMessageRequest(
            text = text.ifBlank { null },
            image = mediaUrl
        )
        chatViewModel.sendMessage(sendReq, currentUserId)
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
                items(
                    items = messages,
                    key = { message -> message.id }
                ) { message ->
                    if (
                        message.timestamp != null &&
                        message.text.isEmpty() &&
                        message.mediaUrl == null &&
                        message.emotion == null
                    ) {
                        TimeDivider(time = message.timestamp)
                    } else {
                        MessageBubble(
                            message = message,
                            senderName = currentChatName,
                            onImageClick = { url -> previewImageUrl = url },
                            onReactionSelect = { msg, reaction ->
                                val currentReaction = reactionByMessageId[msg.id]
                                if (currentReaction == reaction) {reactionByMessageId.remove(msg.id)} 
                                else {reactionByMessageId[msg.id] = reaction}
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
            onAddMemberClick = { showAddMemberDialog = true },
            onChangeGroupNameClick = { showChangeGroupNameDialog = true }
        )
    }

    if (showChangeGroupNameDialog && isGroup) {
        ChangeGroupNameDialog(
            currentName = currentChatName,
            onDismiss = { showChangeGroupNameDialog = false },
            onSave = { newName ->
                showChangeGroupNameDialog = false
                currentChatName = newName
                //updateGroupName(context, chatId, newName)
            }
        )
    }

    if (showIndividualSettingsDialog && !isGroup) {
        IndividualChatSettingsDialog(
            context = context,
            friendId = chatId,
            friendName = currentChatName,
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
                //updateGroupMembers(context, chatId, groupMembers.toList())
            }
        )
    }
}