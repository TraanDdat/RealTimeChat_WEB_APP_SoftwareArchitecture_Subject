package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.ChatMessage
import com.example.zaloclone.api.model.ChatUser
import com.example.zaloclone.api.model.SendMessageRequest

interface IChatRepository {
    suspend fun getContacts(): List<ChatUser>
    suspend fun getChatPartners(): List<ChatUser>
    suspend fun getMessages(userId: Int): List<ChatMessage>
    suspend fun sendMessage(receiverId: Int, data: SendMessageRequest): ChatMessage
}