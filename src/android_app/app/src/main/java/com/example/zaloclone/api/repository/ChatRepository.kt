package com.example.zaloclone.api.repository

import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.api.remote.ChatApiServices
import com.example.zaloclone.api.model.ChatUser
import com.example.zaloclone.api.model.ChatMessage
import com.example.zaloclone.api.model.Conversation
import com.example.zaloclone.api.model.SendMessageRequest

class ChatRepository(
    private val chatApi: ChatApiServices = RetrofitClient.ChatApi
) : IChatRepository {
    override suspend fun getContacts(): List<ChatUser> = chatApi.getContacts()

    override suspend fun getChatPartners(): List<ChatUser> = chatApi.getChatPartners()

    override suspend fun getConversations(): List<Conversation> = chatApi.getConversations()

    override suspend fun getMessages(userId: Int): List<ChatMessage> = chatApi.getMessages(userId)

    override suspend fun getMessagesByConversation(conversationId: String): List<ChatMessage> = chatApi.getMessagesByConversation(conversationId)

    override suspend fun sendMessage(receiverId: Int, data: SendMessageRequest): ChatMessage = chatApi.sendMessage(receiverId, data)

    override suspend fun sendMessageToConversation(conversationId: String, data: SendMessageRequest): ChatMessage = chatApi.sendMessageToConversation(conversationId, data)

    override suspend fun deleteFriend(friendId: Int) = chatApi.deleteFriend(friendId)
}
