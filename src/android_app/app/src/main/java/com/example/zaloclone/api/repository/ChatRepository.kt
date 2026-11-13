package com.example.zaloclone.api.repository

import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.api.model.ChatUser
import com.example.zaloclone.api.model.ChatMessage
import com.example.zaloclone.api.model.SendMessageRequest

class ChatRepository {
    suspend fun getContacts(): List<ChatUser> { return RetrofitClient.ChatApi.getContacts() }

    suspend fun getChatPartners(): List<ChatUser> { return RetrofitClient.ChatApi.getChatPartners() }

    suspend fun getMessages(userId: Int): List<ChatMessage> { return RetrofitClient.ChatApi.getMessages(userId) }

    suspend fun sendMessage( receiverId: Int, data: SendMessageRequest ): ChatMessage { return RetrofitClient.ChatApi.sendMessage(receiverId, data) }
}