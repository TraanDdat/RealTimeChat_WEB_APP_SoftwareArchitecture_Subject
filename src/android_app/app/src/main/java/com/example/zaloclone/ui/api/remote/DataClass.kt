package com.example.zaloclone.ui.api.remote

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

data class ChatUser(
    @SerializedName("_id") val id: Long,
    val username: String,
    val email: String,
    val profilePic: String? = null
)

data class LoginRequest(val email: String, val password: String, val role: String = "User")

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: String = "User"
)

data class AuthResult(val email: String, val username: String, val role: String)
data class AuthEnvelope(val data: AuthResult, val error_code: String? = null)

data class ChatMessage(
    @SerializedName("_id") val id: String,
    val senderId: Long,
    val receiverId: Long,
    val text: String? = null,
    val image: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
) {
    fun belongsTo(currentUserId: Long, partnerId: Long): Boolean =
        (senderId == currentUserId && receiverId == partnerId) ||
            (senderId == partnerId && receiverId == currentUserId)
}

data class SendMessageRequest(val text: String, val image: String? = null)

fun mergeMessages(history: List<ChatMessage>, live: List<ChatMessage>): List<ChatMessage> =
    (history + live).distinctBy { it.id }.sortedBy { OffsetDateTime.parse(it.createdAt).toInstant() }