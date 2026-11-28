
package com.example.zaloclone.api.model

import com.google.gson.annotations.SerializedName

data class GroupCreateRequest(
    val name: String,
    @SerializedName("member_ids")
    val member_ids: List<Int>
)

enum class GroupMemberRole {
    MEMBER,
    ADMIN
}

data class GroupMemberAddRequest(
    val user_id: Int,
    val role: String = "member"
)

data class GroupMemberRemoveRequest(
    val user_id: Int,
    val role: String = "member"
)

data class GroupMemberLeaveRequest(
    val user_id: Int,
    val role: String = "member"
)

data class GroupMemberRoleUpdateRequest(
    val groupId: Int,
    val userId: Int,
    val role: String
)

data class GetGroupMemberResponse(
    val user_id: Int,
    val username: String,
    val email: String,
    val role: String
)

data class GetGroupDataResponse(
    val id: Int,
    val name: String,
    @SerializedName("created_by")
    val created_by: Int,
    @SerializedName("created_at")
    val created_at: String,
    val members: List<GetGroupMemberResponse>
)