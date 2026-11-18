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
    val createdAt: String,
    val updatedAt: String? = null,
    val isOptimistic: Boolean? = false
)

data class SendMessageRequest(
    val text: String?,
    val image: String?
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