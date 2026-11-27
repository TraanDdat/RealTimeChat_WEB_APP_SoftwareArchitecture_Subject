package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.*
import com.example.zaloclone.ApiConfig

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

interface FriendApiServices {

    @POST(ApiConfig.USER_ADDFRIEND_ADD_URL)
    suspend fun sendRequest(@Body data: AddFriendRequest): AddFriendResponse

    @GET(ApiConfig.USER_ADDFRIEND_GET_REQUEST_URL)
    suspend fun getRequests(): FriendListResponse

    @POST("/api/user/addfriend/requests/{friendship_id}/accept")
    suspend fun acceptRequest(@Path("friendship_id") friendship_id: Int): FriendRequestResponse

    @POST("/api/user/addfriend/requests/{friendship_id}/reject")
    suspend fun rejectRequest( @Path("friendship_id") friendship_id: Int): FriendRequestResponse

    @POST(ApiConfig.USER_ADDFRIEND_DELETE_URL)
    suspend fun deleteFriendshipRequest( @Query("other_user_id") otherUserId: Int ): Response<Unit>

    @GET(ApiConfig.USER_ADDFRIEND_GET_REQUEST_SENT_URL)
    suspend fun getPendingRequest(): FriendListResponse
}