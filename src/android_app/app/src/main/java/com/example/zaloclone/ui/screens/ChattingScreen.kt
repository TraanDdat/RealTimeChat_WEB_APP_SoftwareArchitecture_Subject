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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.model.SendMessageDataRequest
import com.example.zaloclone.data.local.*
import com.example.zaloclone.ui.model.Message
import com.example.zaloclone.ui.model.MessageType
import com.example.zaloclone.ui.screens.chatting.*
import com.example.zaloclone.ui.viewModel.*
import android.util.Base64
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect

@Composable
fun ChattingScreen(
    chatId: String,
    chatName: String,
    chatViewModel: ChatViewModel,
    groupViewModel: GroupViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val groupState by groupViewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val chatState by chatViewModel.state.collectAsState()
    val allContacts = chatState.allContacts
    val currentUserId = chatViewModel.getCurrentUserId()?: return
    val friendId = chatId.toIntOrNull()
    val reactionByMessageId = remember {mutableStateMapOf<String, String>()}
    var currentChatName by remember { mutableStateOf(chatName) }
    var showChangeGroupNameDialog by remember { mutableStateOf(false) }
    var showIndividualSettingsDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAttachPanel by remember { mutableStateOf(false) }
    var showEmotionSheet by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var forwardingMessageId by remember { mutableStateOf<String?>(null) }
    var isForwarding by remember { mutableStateOf(false) }
    val isGroup = remember(chatId) {chatId.startsWith("group_")}
    val groupId = remember(chatId) {chatId.removePrefix("group_").toIntOrNull()}
    val groupMembers = remember {mutableStateListOf<GetGroupMemberResponse>()}

    DisposableEffect(friendId, isGroup) {
        if (!isGroup && friendId != null) {
            chatViewModel.setCurrentChatUser(friendId)
        } else {
            chatViewModel.setCurrentChatUser(null)
        }
        onDispose {
            chatViewModel.setCurrentChatUser(null)
        }
    }

    LaunchedEffect(chatId) {
        if (chatName.isNotBlank()) {
            currentChatName = chatName
        }
        if (isGroup) {
            groupId?.let {
                chatViewModel.clearGroupUnreadCount(it)
            }
        } else {
            friendId?.let {
                chatViewModel.clearUnreadCount(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        groupViewModel.getGroups()
    }

    LaunchedEffect(groupState.groups, groupId) {
        val selectedGroup = groupState.groups.firstOrNull {
            it.id == groupId
        }
        groupMembers.clear()
        groupMembers.addAll(selectedGroup?.members.orEmpty())
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
        Log.d("CHAT_DEBUG", "chatId = $chatId")
        reactionByMessageId.clear()

        if(isGroup) {
            val groupId = chatId.removePrefix("group_").toIntOrNull()
            groupId?.let {
                chatViewModel.clearNotificationsForGroup(groupId)
                chatViewModel.setSelectedGroup(groupId)
            }
            //groupMembers.add("Me")
        } else {
            val userId = chatId.toIntOrNull()
            if (userId != null) {
                val targetUser = ChatUser(
                    id = userId,
                    username = chatName,
                    email = "",
                    profilePic = null
                )
                chatViewModel.clearNotificationsForUser(userId)
                chatViewModel.setSelectedUser(targetUser)
            }
        }
    }

    val messages = remember(
        chatState.messages,
        currentUserId,
        reactionByMessageId.toMap()
    ) {
        chatState.messages.map { chatMsg ->
            val isMe = chatMsg.senderId == currentUserId
            val messageType = when {
                !chatMsg.image.isNullOrBlank() -> MessageType.IMAGE
                !chatMsg.attachment.isNullOrBlank() && chatMsg.attachmentMimeType?.startsWith("video/") == true -> MessageType.VIDEO
                !chatMsg.attachment.isNullOrBlank() -> MessageType.DOCUMENT
                !chatMsg.emotion.isNullOrBlank() -> MessageType.EMOTION
                else -> MessageType.TEXT
            }
            Message(
                id = chatMsg.id,
                text = chatMsg.text ?: "",
                isMe = isMe,
                type = messageType,
                image = chatMsg.image,
                timestamp = chatMsg.createdAt,
                emotion = chatMsg.emotion,
                mediaUrl = chatMsg.image ?: chatMsg.attachment,
                fileName = chatMsg.attachmentName,
                reaction = if (chatMsg.isRecalled) null else reactionByMessageId[chatMsg.id],
                isRecalled = chatMsg.isRecalled,
                isForwarded = chatMsg.isForwarded
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
        emotion: String? = null,
        attachment: String? = null,
        attachmentName: String? = null,
        attachmentMimeType: String? = null
    ) {
        val isImage = type == MessageType.IMAGE
        val sendReq = SendMessageDataRequest(
            text = text.ifBlank { null },
            image = if (isImage) mediaUrl else null,
            emotion = if (type == MessageType.EMOTION) emotion else null,
            attachment = if (isImage) null else (attachment ?: mediaUrl),
            attachmentName = if (isImage) null else attachmentName,
            attachmentMimeType = if (isImage) null else attachmentMimeType
        )
        chatViewModel.sendMessage(sendReq, currentUserId)
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val imageDataUrl = uriToBase64DataUrl(context, it)
            sendMediaMessage(
                type = MessageType.IMAGE,
                mediaUrl = imageDataUrl,
                attachmentMimeType = context.contentResolver.getType(it)
            )
            showAttachPanel = false
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fName, fSize) = getFileNameAndSizeFromUri(context, it)
            val videoDataUrl = uriToBase64DataUrl(context, it)
            sendMediaMessage(
                type = MessageType.VIDEO,
                mediaUrl = videoDataUrl,
                attachmentName = fName,
                attachmentMimeType = context.contentResolver.getType(it)
            )
            showAttachPanel = false
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fName, fSize) = getFileNameAndSizeFromUri(context, it)
            val documentDataUrl = uriToBase64DataUrl(context, it)
            sendMediaMessage(
                type = MessageType.DOCUMENT,
                mediaUrl = documentDataUrl,
                attachmentName = fName,
                attachmentMimeType = context.contentResolver.getType(it)
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
                selectGroup = groupState.groups.firstOrNull {
                    it.id == chatId.removePrefix("group_").toIntOrNull()
                },
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

                // Typing indicator
                if (!isGroup && chatState.isOtherUserTyping) {
                    Text(
                        text = "$currentChatName đang nhập...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 4.dp,
                            bottom = 6.dp
                        )
                    )
                }
		
                ChatInputBar(
                    text = messageText,
                    onTextChange = { text ->
                        messageText = text
                        if (!isGroup && friendId != null) {
                            chatViewModel.onMessageInputChanged(
                                receiverId = friendId,
                                message = text
                            )
                        }
                    },
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
                        !message.isRecalled &&
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
                            },
                            onRecall = { msg -> chatViewModel.recallMessage(msg.id) },
                            onForward = { msg ->
                                if (allContacts.isEmpty()) chatViewModel.getAllContacts()
                                forwardingMessageId = msg.id
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

    forwardingMessageId?.let { messageId ->
        ForwardMessageDialog(
            contacts = allContacts.filter { it.id != currentUserId },
            groups = groupState.groups,
            isSending = isForwarding,
            onDismiss = { forwardingMessageId = null },
            onForward = { receiverIds, groupIds ->
                isForwarding = true
                chatViewModel.forwardMessage(messageId, receiverIds, groupIds) { error ->
                    isForwarding = false
                    if (error == null) {
                        forwardingMessageId = null
                        val total = receiverIds.size + groupIds.size
                        Toast.makeText(context, "Đã chuyển tiếp tới $total cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showGroupInfoDialog && isGroup) {
        GroupInfoDialog(
            groupName = currentChatName,
            members = groupMembers.map { it.username },
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

    if (showIndividualSettingsDialog && !isGroup && friendId != null) {
        IndividualChatSettingsDialog(
            context = context,
            friendId = friendId,
            friendName = currentChatName,
            chatViewModel = chatViewModel,
            onDismiss = { showIndividualSettingsDialog = false },
            onNicknameUpdated = { newDisplayName ->
                showIndividualSettingsDialog = false
                currentChatName = newDisplayName
            }
        )
    }

    if (showAddMemberDialog && isGroup) {
        AddMemberDialog(
            currentMembers = groupMembers.map { it.user_id },
            allContacts = allContacts,
            onDismiss = { showAddMemberDialog = false },
            onMembersAdded = { newIds ->
                showAddMemberDialog = false
                newIds.forEach { userId ->
                    groupId?.let { id ->
                        groupViewModel.addGroupMember(
                            groupId = id,
                            userId = userId
                        ) { success: Boolean ->
                            if (success) {
                                groupViewModel.getGroups()
                            }
                        }
                    }
                }
            }
        )
    }
}

fun uriToBase64DataUrl(context: Context, uri: Uri): String {
    val mimeType = context.contentResolver.getType(uri)
        ?: "application/octet-stream"

    val bytes = context.contentResolver.openInputStream(uri)?.use {
        it.readBytes()
    } ?: throw IllegalStateException("Cannot read file")

    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

    return "data:$mimeType;base64,$base64"
}