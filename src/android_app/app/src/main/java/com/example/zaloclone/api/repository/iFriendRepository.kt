package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*
import retrofit2.Response

interface IFriendRepository {
    suspend fun sendRequest(data: AddFriendRequest): AddFriendResponse
    suspend fun getRequests(): FriendListResponse
    suspend fun acceptRequest(friendship_id: Int): FriendRequestResponse
    suspend fun rejectRequest(friendship_id: Int): FriendRequestResponse
    suspend fun deleteFriendshipRequest(friendship_id: Int): Response<Unit>
    suspend fun getPendingRequest(): FriendListResponse
}