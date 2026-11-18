package com.example.zaloclone.ui.model

import androidx.compose.ui.graphics.Color

enum class MessageType { TEXT, AUDIO, IMAGE, VIDEO, DOCUMENT, EMOTION }

enum class PresenceStatus {
    ONLINE,
    OFFLINE;
    fun getDisplayName(): String {
        return when (this) {
            ONLINE -> "Đang hoạt động"
            OFFLINE -> "Ngoại tuyến"
        }
    }
}

data class UserPresence(
    val userId: Int,
    val status: PresenceStatus = PresenceStatus.OFFLINE,
    val lastSeen: String? = null
)

data class Message(
    val id: String,
    val text: String = "",
    val isMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val timestamp: String,
    val duration: String? = null,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val emotion: String? = null,
    val reaction: String? = null,
    val image: String? = null
)

fun getPreviewTextForMessage(message: Message): String {
    return when (message.type) {
        MessageType.TEXT -> message.text
        MessageType.AUDIO -> "[Ghi âm]"
        MessageType.IMAGE -> "[Hình ảnh]"
        MessageType.VIDEO -> "[Video]"
        MessageType.DOCUMENT -> "[Tài liệu] ${message.fileName ?: ""}".trim()
        MessageType.EMOTION -> "${message.emotion ?: "😊"} [Nhãn dán]"
    }
}

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarBgColor: Color = Color(0xFFE5E5EA),
    val isGroup: Boolean = false,
    val members: List<String>? = emptyList(),
    val avatarUri: String? = null,
    val presenceStatus: PresenceStatus = PresenceStatus.OFFLINE,
    val lastSeen: String? = null
) {
    val safeMembers: List<String>
        get() = members ?: emptyList()

    val safeAvatarBgColor: Color
        get() = avatarBgColor ?: Color(0xFFE5E5EA)
}

data class FriendItem(
    val id: Int,
    val name: String,
    val status: String,
    val isOnline: Boolean = false,
    val email: String = "",
    val phone: String = "",
    val avatarBgColor: Color = Color(0xFFD1E3FF),
    val avatarUri: String? = null,
    val nickname: String = "",
    val presenceStatus: PresenceStatus = PresenceStatus.OFFLINE,
    val lastSeen: String? = null
) {
    val safeAvatarBgColor: Color
        get() = avatarBgColor ?: Color(0xFFD1E3FF)
}

data class UserProfile(
    val name: String = "Người dùng Zalo",
    val email: String = "user@gmail.com",
    val phone: String = "0901234567",
    val password: String = "123456",
    val avatarBgColorHex: Long = 0xFF38C7FF,
    val avatarUri: String? = null
)