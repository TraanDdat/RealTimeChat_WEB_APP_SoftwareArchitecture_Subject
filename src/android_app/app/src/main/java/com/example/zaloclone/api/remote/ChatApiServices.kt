package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.ChatUser
import com.example.zaloclone.api.model.ChatMessage
import com.example.zaloclone.api.model.Conversation
import com.example.zaloclone.api.model.SendMessageRequest
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body
import com.example.zaloclone.ApiConfig

interface ChatApiServices {
    @GET(ApiConfig.API_MSG_CONTACTS)
    suspend fun getContacts(): List<ChatUser>

    @GET(ApiConfig.API_MSG_CHATS)
    suspend fun getChatPartners(): List<ChatUser>

    @GET("conversations")
    suspend fun getConversations(): List<Conversation>

    @GET("messages/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: Int
    ): List<ChatMessage>

    @GET("messages/conversation/{conversationId}")
    suspend fun getMessagesByConversation(
        @Path("conversationId") conversationId: String
    ): List<ChatMessage>

    @POST("messages/send/{receiverId}")
    suspend fun sendMessage(
        @Path("receiverId") receiverId: Int,
        @Body data: SendMessageRequest
    ): ChatMessage

    @POST("messages/send/conversation/{conversationId}")
    suspend fun sendMessageToConversation(
        @Path("conversationId") conversationId: String,
        @Body data: SendMessageRequest
    ): ChatMessage

    @DELETE("friends/{friendId}")
    suspend fun deleteFriend(
        @Path("friendId") friendId: Int
    )
}
