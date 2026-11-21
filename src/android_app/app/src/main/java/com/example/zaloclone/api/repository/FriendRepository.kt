package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.remote.FriendApiServices
import com.example.zaloclone.api.remote.RetrofitClient
import retrofit2.Response

class FriendRepository(
    private val friendApi: FriendApiServices = RetrofitClient.friendApi
) : IFriendRepository {

    override suspend fun sendRequest(data: AddFriendRequest): AddFriendResponse = friendApi.sendRequest(data)

    override suspend fun getRequests(): FriendListResponse = friendApi.getRequests()

    override suspend fun acceptRequest(friendship_id: Int): FriendRequestResponse = friendApi.acceptRequest(friendship_id)

    override suspend fun rejectRequest(friendship_id: Int): FriendRequestResponse = friendApi.rejectRequest(friendship_id)

    override suspend fun deleteFriendshipRequest(otherUserId: Int): Response<Unit> = friendApi.deleteFriendshipRequest(otherUserId)
}