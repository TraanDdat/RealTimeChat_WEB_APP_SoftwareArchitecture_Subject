package com.example.zaloclone.api.repository

import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.api.remote.ChatApiServices
import com.example.zaloclone.api.model.*

class ChatRepository(
    private val chatApi: ChatApiServices = RetrofitClient.chatApi
) : IChatRepository {
    override suspend fun getContacts(): List<ChatUser> = chatApi.getContacts()

    override suspend fun getChatPartners(): List<ChatUser> = chatApi.getChatPartners()

    override suspend fun getMessages(userId: Int): List<ChatMessage> = chatApi.getMessages(userId)

    override suspend fun sendMessage(receiverId: Int, data: SendMessageDataRequest): ChatMessage = chatApi.sendMessage(receiverId, data)

    override suspend fun getGroupMessages(groupId: Int): List<GroupMessageDataResponse> = chatApi.getGroupMessages(groupId)

    override suspend fun sendGroupMessage(groupId: Int, data: SendMessageDataRequest): GroupMessageDataResponse = chatApi.sendGroupMessage(groupId, data)

    override suspend fun deleteFriend(friendId: Int) = chatApi.deleteFriend(friendId)

    override suspend fun getUnreadCounts(): UnreadCountsResponse = chatApi.getUnreadCounts()

    override suspend fun markRead(userId: Int) {chatApi.markRead(userId)}
}
