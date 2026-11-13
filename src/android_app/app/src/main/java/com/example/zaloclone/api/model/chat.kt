package com.example.zaloclone.api.model

import com.google.gson.annotations.SerializedName

data class ChatUser(
    @SerializedName("_id") val id: Int,
    val username: String,
    val email: String,
    val profilePic: String?
)

data class ChatMessage(
    @SerializedName("_id") val id: Int,
    val senderId: Int,
    val receiverId: Int,
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