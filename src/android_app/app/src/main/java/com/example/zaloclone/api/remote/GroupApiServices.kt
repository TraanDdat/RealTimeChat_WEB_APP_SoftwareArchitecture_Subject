package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.*
import com.example.zaloclone.ApiConfig

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface GroupApiServices {
    @GET(ApiConfig.API_GROUP_URL)
    suspend fun getGroups(): List<GetGroupDataResponse>

    @POST(ApiConfig.API_GROUP_URL)
    suspend fun createGroup(@Body data: GroupCreateRequest): GetGroupDataResponse

    @POST("api/groups/{groupId}/add/members")
    suspend fun addMember(
        @Path("groupId") groupId: Int,
        @Body data: GroupMemberAddRequest
    ): GetGroupDataResponse

    @POST("api/groups/{groupId}/remove/members")
    suspend fun removeMember(
        @Path("groupId") groupId: Int,
        @Body data: GroupMemberRemoveRequest
    ): GetGroupDataResponse

    @POST("api/groups/{groupId}/leave/members")
    suspend fun leaveMember(
        @Path("groupId") groupId: Int,
        @Body data: GroupMemberLeaveRequest
    ): GetGroupDataResponse

    @PATCH("api/groups/{groupId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("groupId") groupId: Int,
        @Path("userId") userId: Int,
        @Body data: String
    ): GetGroupDataResponse
}