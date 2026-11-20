package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.*
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

    @GET("/api/messages/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: Int
    ): List<ChatMessage>

    @POST("/api/messages/send/{receiverId}")
    suspend fun sendMessage(
        @Path("receiverId") receiverId: Int,
        @Body data: SendMessageDataRequest
    ): ChatMessage

    @DELETE("friends/{friendId}")
    suspend fun deleteFriend(
        @Path("friendId") friendId: Int
    )

    @GET("api/groups/{groupId}/messages")
    suspend fun getGroupMessages(@Path("groupId") groupId: Int): List<GroupMessageDataResponse>

    @POST("api/groups/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: Int,
        @Body data: SendMessageDataRequest
    ): GroupMessageDataResponse

    @GET("api/messages/unread-counts")
    suspend fun getUnreadCounts(): UnreadCountsResponse

    @POST("api/messages/{userId}/read")
    suspend fun markRead(
        @Path("userId") userId: Int
    )
}
