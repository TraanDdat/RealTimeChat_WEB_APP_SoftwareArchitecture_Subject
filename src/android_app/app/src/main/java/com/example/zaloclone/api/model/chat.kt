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
    val emotion: String? = null,
    val isRecalled: Boolean = false,
    val isForwarded: Boolean = false
) {
    /** Mirrors backend `recalled_message_fields()`: content is wiped, the record stays as a placeholder. */
    fun asRecalled(): ChatMessage = copy(
        isRecalled = true,
        text = null,
        image = null,
        emotion = null,
        attachment = null,
        attachmentName = null,
        attachmentMimeType = null
    )
}

data class MessageDeletedEvent(
    val messageId: String,
    val senderId: Int? = null,
    val groupId: Int? = null
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
    val emotion: String? = null,
    val attachment: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val isRecalled: Boolean = false,
    val isForwarded: Boolean = false
)

/** Body of POST /api/messages/forward. `sourceGroupId` is set when the original lives in a group. */
data class ForwardMessageRequest(
    val messageId: String,
    val sourceGroupId: Int?,
    val receiverIds: List<Int>,
    val groupIds: List<Int>
)

data class ForwardMessageResponse(
    val messages: List<ChatMessage> = emptyList(),
    val groupMessages: List<GroupMessageDataResponse> = emptyList()
)

data class UnreadCountsResponse(
    val users: Map<String, Int> = emptyMap(),
    val groups: Map<String, Int> = emptyMap()
)

data class TypingEvent(
    val sender_id: Int
)