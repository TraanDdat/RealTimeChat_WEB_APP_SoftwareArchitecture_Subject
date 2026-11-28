package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.remote.GroupApiServices
import com.example.zaloclone.api.remote.RetrofitClient

class GroupRepository(
    private val groupdApi: GroupApiServices = RetrofitClient.groupdApi
) : IGroupRepository {

    override suspend fun getGroups(): List<GetGroupDataResponse> {
        return groupdApi.getGroups()
    }

    override suspend fun createGroup(
        data: GroupCreateRequest
    ): GetGroupDataResponse {
        return groupdApi.createGroup(data)
    }

    override suspend fun addMember(
        groupId: Int,
        data: GroupMemberAddRequest
    ): GetGroupDataResponse {
        return groupdApi.addMember(groupId, data)
    }

    override suspend fun removeMember(
        groupId: Int,
        data: GroupMemberRemoveRequest
    ): GetGroupDataResponse {
        return groupdApi.removeMember(groupId, data)
    }

    override suspend fun leaveMember(
        groupId: Int,
        data: GroupMemberLeaveRequest
    ): GetGroupDataResponse {
        return groupdApi.leaveMember(groupId, data)
    }

    override suspend fun updateMemberRole(
        groupId: Int,
        userId: Int,
        data: String
    ): GetGroupDataResponse {
        return groupdApi.updateMemberRole(groupId, userId, data)
    }
}