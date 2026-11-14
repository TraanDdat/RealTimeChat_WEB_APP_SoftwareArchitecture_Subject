package com.example.zaloclone.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.zaloclone.ui.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val inMemoryMessagesMap = mutableMapOf<String, MutableList<Message>>()

fun saveMessagesToJson(context: Context, chatId: String, messages: List<Message>) {
    inMemoryMessagesMap[chatId] = messages.toMutableList()
}

fun loadMessagesFromJson(context: Context, chatId: String): List<Message> {
    return inMemoryMessagesMap[chatId] ?: emptyList()
}

fun getOrInitChatHistory(context: Context, chatId: String, chatName: String): List<Message> {
    return loadMessagesFromJson(context, chatId)
}

const val CHAT_LIST_FILE = "chat_list.json"
private var inMemoryChatList: MutableList<ChatItem> = mutableListOf()

fun saveChatListToJson(context: Context, chatList: List<ChatItem>) {
    inMemoryChatList = chatList.toMutableList()
}

fun loadChatListFromJson(context: Context): List<ChatItem> {
    if (inMemoryChatList.isNotEmpty()) {
        return inMemoryChatList
    }
    val file = File(context.filesDir, CHAT_LIST_FILE)
    if (!file.exists()) {
        return emptyList()
    }
    val type = object : TypeToken<List<ChatItem>>() {}.type
    val rawList: List<ChatItem> = Gson().fromJson(file.readText(), type) ?: emptyList()
    return rawList.map { item ->
        item.copy(
            name = item.name ?: "",
            lastMessage = item.lastMessage ?: "",
            time = item.time ?: "",
            members = item.members ?: emptyList()
        )
    }
}

fun syncChatListWithFriends(context: Context) {
    val friendList = loadFriendListFromJson(context)
    val chatList = loadChatListFromJson(context).toMutableList()
    var modified = false

    val friendMap = friendList.associateBy { it.id }

    for (i in chatList.indices) {
        val chat = chatList[i]
        val friend = friendMap[chat.id] ?: friendList.find { it.name == chat.name }
        if (friend != null && !chat.isGroup) {
            val friendBgColor = friend.safeAvatarBgColor
            val friendAvatarUri = friend.avatarUri
            val displayName = if (friend.nickname.isNotBlank()) friend.nickname else friend.name

            if (chat.safeAvatarBgColor != friendBgColor || chat.avatarUri != friendAvatarUri || chat.name != displayName) {
                chatList[i] = chat.copy(
                    name = displayName,
                    avatarBgColor = friendBgColor,
                    avatarUri = friendAvatarUri
                )
                modified = true
            }
        }
    }

    if (modified) {
        saveChatListToJson(context, chatList)
    }
}

fun syncChatListWithHistory(context: Context) {
    syncChatListWithFriends(context)
    val chatList = loadChatListFromJson(context).toMutableList()
    var modified = false

    for (i in chatList.indices) {
        val chat = chatList[i]
        val messages = loadMessagesFromJson(context, chat.id)
        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            val textContent = getPreviewTextForMessage(lastMsg)
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

const val FRIEND_LIST_FILE = "friend_list.json"

fun saveFriendListToJson(context: Context, friendList: List<FriendItem>) {
    File(context.filesDir, FRIEND_LIST_FILE).writeText(Gson().toJson(friendList))
}

fun loadFriendListFromJson(context: Context): List<FriendItem> {
    val file = File(context.filesDir, FRIEND_LIST_FILE)
    if (!file.exists()) {
        val defaultList = listOf(
            FriendItem("1", "Anny Peter", "Online", true, "annypeter@gmail.com", "0901234567", Color(0xFFE2C4B1)),
            FriendItem("2", "John Doe", "Online", true, "johndoe@gmail.com", "0902345678", Color(0xFFD1E3FF)),
            FriendItem("3", "Arnold Peter", "Last seen at 6:20 PM", false, "arnoldpeter@gmail.com", "0903456789", Color(0xFFE0C1C6)),
            FriendItem("4", "Dann Jazmyn", "Last seen at 6:20 PM", false, "dannjazmyn@gmail.com", "0904567890", Color(0xFFE5E5EA)),
            FriendItem("5", "Duane Brodie", "Online", true, "duanebrodie@gmail.com", "0905678901", Color(0xFFD7BDE2)),
            FriendItem("6", "Damion Gage", "Last seen at 6:20 PM", false, "damiongage@gmail.com", "0906789012", Color(0xFFBDC3C7)),
            FriendItem("7", "Demarcus Jazmyn", "Online", true, "demarcus@gmail.com", "0907890123", Color(0xFFD0D3D4)),
            FriendItem("8", "Emmalee Alivia", "Last seen at 6:20 PM", false, "emmalee@gmail.com", "0908901234", Color(0xFFE5E5EA)),
            FriendItem("9", "Esha Chikere", "Last seen at 6:20 PM", false, "eshachikere@gmail.com", "0909012345", Color(0xFFE0C1C6)),
            FriendItem("10", "Mariam Rowan", "Last seen at 6:20 PM", false, "mariamrowan@gmail.com", "0910123456", Color(0xFFD1E3FF))
        )
        saveFriendListToJson(context, defaultList)
        return defaultList
    }
    val type = object : TypeToken<List<FriendItem>>() {}.type
    val rawList: List<FriendItem> = Gson().fromJson(file.readText(), type) ?: emptyList()
    return rawList.map { friend ->
        friend.copy(
            name = friend.name ?: "",
            status = friend.status ?: "",
            email = friend.email ?: "",
            phone = friend.phone ?: "",
            nickname = friend.nickname ?: ""
        )
    }
}

fun deleteChat(context: Context, chatId: String) {
    val chatList = loadChatListFromJson(context).toMutableList()
    chatList.removeAll { it.id == chatId }
    saveChatListToJson(context, chatList)

    inMemoryMessagesMap.remove(chatId)
}

fun deleteFriend(context: Context, friendId: String) {
    val friendList = loadFriendListFromJson(context).toMutableList()
    friendList.removeAll { it.id == friendId }
    saveFriendListToJson(context, friendList)

    deleteChat(context, friendId)
}

const val FRIEND_REQUESTS_FILE = "friend_requests.json"

fun saveFriendRequestsFromJson(context: Context, requests: List<FriendRequestItem>) {
    File(context.filesDir, FRIEND_REQUESTS_FILE).writeText(Gson().toJson(requests))
}

fun loadFriendRequestsFromJson(context: Context): List<FriendRequestItem> {
    val file = File(context.filesDir, FRIEND_REQUESTS_FILE)
    if (!file.exists()) {
        val defaultList = listOf(
            FriendRequestItem("req_1", "Sarah Connor", "sarah.connor@gmail.com", "0912345678", "Xin chào, kết bạn nhé!", Color(0xFFE2C4B1)),
            FriendRequestItem("req_2", "Michael Scofield", "m.scofield@gmail.com", "0987654321", "Cho mình làm quen với nhé!", Color(0xFFD1E3FF))
        )
        saveFriendRequestsFromJson(context, defaultList)
        return defaultList
    }
    val type = object : TypeToken<List<FriendRequestItem>>() {}.type
    return Gson().fromJson(file.readText(), type) ?: emptyList()
}

fun addFriendAndChat(
    context: Context,
    id: String,
    name: String,
    email: String = "",
    phone: String = "",
    avatarBgColor: Color,
    avatarUri: String? = null
) {
    val friendList = loadFriendListFromJson(context).toMutableList()
    if (friendList.none { it.id == id }) {
        val newFriend = FriendItem(
            id = id,
            name = name,
            status = "Online",
            isOnline = true,
            email = email,
            phone = phone,
            avatarBgColor = avatarBgColor,
            avatarUri = avatarUri
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
            avatarBgColor = avatarBgColor,
            avatarUri = avatarUri
        )
        chatList.add(0, newChat)
        saveChatListToJson(context, chatList)
    }
}

fun updateGroupName(context: Context, groupId: String, newName: String) {
    val chatList = loadChatListFromJson(context).toMutableList()
    val index = chatList.indexOfFirst { it.id == groupId }
    if (index != -1) {
        chatList[index] = chatList[index].copy(name = newName)
        saveChatListToJson(context, chatList)
    }
}

fun updateFriendNickname(context: Context, friendId: String, newNickname: String) {
    val friendList = loadFriendListFromJson(context).toMutableList()
    val index = friendList.indexOfFirst { it.id == friendId }
    if (index != -1) {
        val updated = friendList[index].copy(nickname = newNickname)
        friendList[index] = updated
        saveFriendListToJson(context, friendList)

        val chatList = loadChatListFromJson(context).toMutableList()
        val chatIndex = chatList.indexOfFirst { it.id == friendId }
        if (chatIndex != -1) {
            val displayName = if (newNickname.isNotBlank()) newNickname else updated.name
            chatList[chatIndex] = chatList[chatIndex].copy(name = displayName)
            saveChatListToJson(context, chatList)
        }
    }
}

fun updateGroupMembers(context: Context, groupId: String, newMembers: List<String>) {
    val chatList = loadChatListFromJson(context).toMutableList()
    val index = chatList.indexOfFirst { it.id == groupId }
    if (index != -1) {
        val updated = chatList[index].copy(members = newMembers)
        chatList[index] = updated
        saveChatListToJson(context, chatList)
    }
}

const val USER_PROFILE_FILE = "user_profile.json"

fun saveUserProfileToJson(context: Context, profile: UserProfile) {
    File(context.filesDir, USER_PROFILE_FILE).writeText(Gson().toJson(profile))
}

fun loadUserProfileFromJson(context: Context): UserProfile {
    val file = File(context.filesDir, USER_PROFILE_FILE)
    if (!file.exists()) {
        val defaultProfile = UserProfile()
        saveUserProfileToJson(context, defaultProfile)
        return defaultProfile
    }
    val type = object : TypeToken<UserProfile>() {}.type
    return Gson().fromJson(file.readText(), type) ?: UserProfile()
}