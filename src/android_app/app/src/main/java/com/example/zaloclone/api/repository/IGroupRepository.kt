package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*

interface IGroupRepository {
    suspend fun getGroups(): List<GetGroupDataResponse>

    suspend fun createGroup(data: GroupCreateRequest): GetGroupDataResponse

    suspend fun addMember(groupId: Int, data: GroupMemberAddRequest): GetGroupDataResponse

    suspend fun removeMember(groupId: Int, data: GroupMemberRemoveRequest): GetGroupDataResponse

    suspend fun updateMemberRole(
        groupId: Int,
        userId: Int,
        data: String
    ): GetGroupDataResponse
}