package com.example.zaloclone.api.model

import com.google.gson.annotations.SerializedName

data class ChatUser(
    @SerializedName("_id") val id: Int,
    val username: String,
    val email: String,
    val profilePic: String?
)

data class Conversation(
    @SerializedName("_id") val id: String,
    val name: String?,
    val isGroup: Boolean,
    val avatarUri: String?,
    val members: List<ChatUser>?,
    val createdAt: String?
)

data class ConversationParticipant(
    val conversationId: String,
    val userId: Int,
    val joinedAt: String?
)

data class ChatMessage(
    @SerializedName("_id") val id: Int,
    val senderId: Int,
    val receiverId: Int?,
    val conversationId: String?,
    val text: String?,
    val image: String?,
    val createdAt: String,
    val updateAt: String?,
    val isOptimistic: Boolean?
)

data class SendMessageRequest(
    val text: String?,
    val image: String?
)
