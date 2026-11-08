package com.example.zaloclone.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. DATA MODELS
// ==========================================
data class Message(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val timestamp: String? = null,
    val duration: String? = null
)

enum class MessageType { TEXT, AUDIO }

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarBgColor: Color = Color(0xFFE5E5EA)
)

// ==========================================
// 2. HÀM XỬ LÝ JSON - CHAT HISTORY (Per-chat)
// ==========================================
fun getChatHistoryFileName(chatId: String): String = "chat_history_$chatId.json"

fun saveMessagesToJson(context: Context, chatId: String, messages: List<Message>) {
    File(context.filesDir, getChatHistoryFileName(chatId)).writeText(Gson().toJson(messages))
}

fun loadMessagesFromJson(context: Context, chatId: String): List<Message> {
    val file = File(context.filesDir, getChatHistoryFileName(chatId))
    if (!file.exists()) return emptyList()
    val type = object : TypeToken<List<Message>>() {}.type
    return Gson().fromJson(file.readText(), type) ?: emptyList()
}

fun getOrInitChatHistory(context: Context, chatId: String, chatName: String): List<Message> {
    val savedMessages = loadMessagesFromJson(context, chatId)
    if (savedMessages.isNotEmpty()) {
        return savedMessages
    }

    val chatList = loadChatListFromJson(context).toMutableList()
    var chatItem = chatList.find { it.id == chatId }

    if (chatItem == null) {
        val colors = listOf(Color(0xFFE2C4B1), Color(0xFFD1E3FF), Color(0xFFE0C1C6), Color(0xFFD7BDE2))
        val randomColor = colors[(chatId.toIntOrNull() ?: 0) % colors.size]
        chatItem = ChatItem(
            id = chatId,
            name = chatName,
            lastMessage = "",
            time = "",
            unreadCount = 0,
            avatarBgColor = randomColor
        )
        chatList.add(0, chatItem)
        saveChatListToJson(context, chatList)
    }

    return emptyList()
}

// ==========================================
// 3. HÀM XỬ LÝ JSON - CHAT LIST
// ==========================================
const val CHAT_LIST_FILE = "chat_list.json"

fun saveChatListToJson(context: Context, chatList: List<ChatItem>) {
    File(context.filesDir, CHAT_LIST_FILE).writeText(Gson().toJson(chatList))
}

fun loadChatListFromJson(context: Context): List<ChatItem> {
    val file = File(context.filesDir, CHAT_LIST_FILE)
    if (!file.exists()) {
        val defaultList = listOf(
            ChatItem("1", "Anny Peter", "Why didn't you reply to me? 😔", "15:08", 0, Color(0xFFE2C4B1)),
            ChatItem("2", "John Doe", "Hey, are we still meeting today?", "Yesterday", 2, Color(0xFFD1E3FF)),
            ChatItem("3", "Arnold Peter", "See you tomorrow!", "Mon", 0, Color(0xFFE0C1C6))
        )
        saveChatListToJson(context, defaultList)
        return defaultList
    }
    val type = object : TypeToken<List<ChatItem>>() {}.type
    return Gson().fromJson(file.readText(), type) ?: emptyList()
}

fun syncChatListWithHistory(context: Context) {
    val chatList = loadChatListFromJson(context).toMutableList()
    var modified = false

    for (i in chatList.indices) {
        val chat = chatList[i]
        val messages = loadMessagesFromJson(context, chat.id)
        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            val textContent = lastMsg.text
            val timeContent = lastMsg.timestamp ?: chat.time
            if (chat.lastMessage != textContent) {
                chatList[i] = chat.copy(lastMessage = textContent, time = timeContent)
                modified = true
            }
        }
    }

    if (modified) {
        saveChatListToJson(context, chatList)
    }
}

// ==========================================
// 4. HÀM XỬ LÝ JSON - FRIEND LIST
// ==========================================
data class FriendItem(
    val id: String,
    val name: String,
    val status: String,
    val isOnline: Boolean = false,
    val phone: String = "",
    val avatarBgColor: Color = Color(0xFFD1E3FF)
)

const val FRIEND_LIST_FILE = "friend_list.json"

fun saveFriendListToJson(context: Context, friendList: List<FriendItem>) {
    File(context.filesDir, FRIEND_LIST_FILE).writeText(Gson().toJson(friendList))
}

fun loadFriendListFromJson(context: Context): List<FriendItem> {
    val file = File(context.filesDir, FRIEND_LIST_FILE)
    if (!file.exists()) {
        val defaultList = listOf(
            FriendItem("1", "Anny Peter", "Online", true, "0901234567", Color(0xFFE2C4B1)),
            FriendItem("2", "Arnold Peter", "Last seen at 6:20 PM", false, "0902345678", Color(0xFFE0C1C6)),
            FriendItem("3", "Dann Jazmyn", "Last seen at 6:20 PM", false, "0903456789", Color(0xFFE5E5EA)),
            FriendItem("4", "Duane Brodie", "Online", true, "0904567890", Color(0xFFD7BDE2)),
            FriendItem("5", "Damion Gage", "Last seen at 6:20 PM", false, "0905678901", Color(0xFFBDC3C7)),
            FriendItem("6", "Demarcus Jazmyn", "Online", true, "0906789012", Color(0xFFD0D3D4)),
            FriendItem("7", "Emmalee Alivia", "Last seen at 6:20 PM", false, "0907890123", Color(0xFFE5E5EA)),
            FriendItem("8", "Esha Chikere", "Last seen at 6:20 PM", false, "0908901234", Color(0xFFE0C1C6)),
            FriendItem("9", "Mariam Rowan", "Last seen at 6:20 PM", false, "0909012345", Color(0xFFD1E3FF)),
            FriendItem("10", "Piety Candi", "Online", true, "0910123456", Color(0xFFBDC3C7))
        )
        saveFriendListToJson(context, defaultList)
        return defaultList
    }
    val type = object : TypeToken<List<FriendItem>>() {}.type
    return Gson().fromJson(file.readText(), type) ?: emptyList()
}

fun deleteChat(context: Context, chatId: String) {
    val chatList = loadChatListFromJson(context).toMutableList()
    chatList.removeAll { it.id == chatId }
    saveChatListToJson(context, chatList)

    val file = File(context.filesDir, getChatHistoryFileName(chatId))
    if (file.exists()) {
        file.delete()
    }
}

fun deleteFriend(context: Context, friendId: String) {
    val friendList = loadFriendListFromJson(context).toMutableList()
    friendList.removeAll { it.id == friendId }
    saveFriendListToJson(context, friendList)

    // Đồng thời xóa cuộc trò chuyện của đối tượng đó ở ChatList
    deleteChat(context, friendId)
}

// ==========================================
// 5. HÀM XỬ LÝ JSON - FRIEND REQUESTS
// ==========================================
data class FriendRequestItem(
    val id: String,
    val name: String,
    val phone: String,
    val message: String = "Xin chào, mình muốn kết bạn với bạn.",
    val avatarBgColor: Color = Color(0xFFD1E3FF)
)

const val FRIEND_REQUESTS_FILE = "friend_requests.json"

fun saveFriendRequestsFromJson(context: Context, requests: List<FriendRequestItem>) {
    File(context.filesDir, FRIEND_REQUESTS_FILE).writeText(Gson().toJson(requests))
}

fun loadFriendRequestsFromJson(context: Context): List<FriendRequestItem> {
    val file = File(context.filesDir, FRIEND_REQUESTS_FILE)
    if (!file.exists()) {
        val defaultList = listOf(
            FriendRequestItem("req_1", "Sarah Connor", "0912345678", "Xin chào, kết bạn nhé!", Color(0xFFE2C4B1)),
            FriendRequestItem("req_2", "Michael Scofield", "0987654321", "Cho mình làm quen với nhé!", Color(0xFFD1E3FF))
        )
        saveFriendRequestsFromJson(context, defaultList)
        return defaultList
    }
    val type = object : TypeToken<List<FriendRequestItem>>() {}.type
    return Gson().fromJson(file.readText(), type) ?: emptyList()
}

fun addFriendAndChat(context: Context, id: String, name: String, phone: String, avatarBgColor: Color) {
    val friendList = loadFriendListFromJson(context).toMutableList()
    if (friendList.none { it.id == id }) {
        val newFriend = FriendItem(
            id = id,
            name = name,
            status = "Online",
            isOnline = true,
            phone = phone,
            avatarBgColor = avatarBgColor
        )
        friendList.add(0, newFriend)
        saveFriendListToJson(context, friendList)
    }

    val chatList = loadChatListFromJson(context).toMutableList()
    if (chatList.none { it.id == id }) {
        val newChat = ChatItem(
            id = id,
            name = name,
            lastMessage = "Let's chat!",
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            unreadCount = 0,
            avatarBgColor = avatarBgColor
        )
        chatList.add(0, newChat)
        saveChatListToJson(context, chatList)
    }
}

@Composable
fun CreateGroupDialog(
    context: Context,
    onDismiss: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    val friendList = remember { mutableStateOf(loadFriendListFromJson(context)) }
    val selectedFriends = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhóm chat mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Tên nhóm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Chọn thành viên (${selectedFriends.count { it.value }} đã chọn):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(friendList.value) { friend ->
                        val isSelected = selectedFriends[friend.id] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFriends[friend.id] = !isSelected
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedFriends[friend.id] = checked
                                }
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
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = friend.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupNameInput.isNotBlank()) {
                        val groupId = "group_${System.currentTimeMillis()}"
                        val groupName = groupNameInput.trim()
                        val selectedMemberNames = friendList.value.filter { selectedFriends[it.id] == true }.map { it.name }
                        val membersDescription = if (selectedMemberNames.isNotEmpty()) {
                            "Thành viên: ${selectedMemberNames.joinToString(", ")}"
                        } else {
                            "Nhóm mới"
                        }

                        val colors = listOf(Color(0xFF38C7FF), Color(0xFF5B61F4), Color(0xFF34C759), Color(0xFFFF9500), Color(0xFFAF52DE))
                        val randomColor = colors[groupId.hashCode().let { if (it < 0) -it else it } % colors.size]

                        val newChatItem = ChatItem(
                            id = groupId,
                            name = groupName,
                            lastMessage = membersDescription,
                            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                            unreadCount = 0,
                            avatarBgColor = randomColor
                        )

                        val currentChats = loadChatListFromJson(context).toMutableList()
                        currentChats.add(0, newChatItem)
                        saveChatListToJson(context, currentChats)

                        onGroupCreated(groupId, groupName)
                    }
                },
                enabled = groupNameInput.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}






