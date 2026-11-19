package com.example.zaloclone.api.model

import com.google.gson.annotations.SerializedName

data class ChatUser(
    @SerializedName("_id") val id: Int,
    val username: String,
    val email: String,
    val profilePic: String?
)

data class ChatMessage(
    @SerializedName("_id") val id: String,
    val senderId: Int,
    val receiverId: Int? = null,
    val groupId: Int? = null,
    val senderUsername: String? = null,
    val text: String?,
    val image: String?,
    val attachment: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val isOptimistic: Boolean? = false,
    val emotion: String? = null
)

data class SendMessageDataRequest(
    val text: String?,
    val image: String?,
    val emotion: String?,
    val attachment: String?,
    val attachmentName: String?,
    val attachmentMimeType: String?
)

data class GroupMessageDataResponse(
    @SerializedName("_id")
    val id: String,
    val groupId: Int,
    val senderId: Int,
    val senderUsername: String? = null,
    val text: String? = null,
    val image: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)