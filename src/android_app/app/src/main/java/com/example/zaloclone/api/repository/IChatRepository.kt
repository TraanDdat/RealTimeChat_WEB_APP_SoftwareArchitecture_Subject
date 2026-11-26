package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*

interface IChatRepository {
    suspend fun getContacts(): List<ChatUser>
    suspend fun getChatPartners(): List<ChatUser>
    suspend fun getMessages(userId: Int): List<ChatMessage>
    suspend fun sendMessage(receiverId: Int, data: SendMessageDataRequest): ChatMessage
    suspend fun getGroupMessages(groupId: Int): List<GroupMessageDataResponse>
    suspend fun sendGroupMessage(groupId: Int, data: SendMessageDataRequest): GroupMessageDataResponse
    suspend fun getUnreadCounts(): UnreadCountsResponse
    suspend fun markRead(userId: Int)
    suspend fun markGroupRead(groupId: Int)
    suspend fun recallMessage(messageId: String)
    suspend fun recallGroupMessage(groupId: Int, messageId: String)
}
