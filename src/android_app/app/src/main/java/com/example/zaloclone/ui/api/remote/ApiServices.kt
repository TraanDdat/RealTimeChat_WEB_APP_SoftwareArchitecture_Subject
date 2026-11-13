package com.example.zaloclone.ui.api.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiServices {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthEnvelope

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthEnvelope

    @GET("api/auth/check")
    suspend fun checkAuth(): ChatUser

    @GET("api/messages/contacts")
    suspend fun contacts(): List<ChatUser>

    @GET("api/messages/chats")
    suspend fun chats(): List<ChatUser>

    @GET("api/messages/{partnerId}")
    suspend fun messages(@Path("partnerId") partnerId: Long): List<ChatMessage>

    @POST("api/messages/send/{receiverId}")
    suspend fun send(
        @Path("receiverId") receiverId: Long,
        @Body request: SendMessageRequest
    ): ChatMessage
}