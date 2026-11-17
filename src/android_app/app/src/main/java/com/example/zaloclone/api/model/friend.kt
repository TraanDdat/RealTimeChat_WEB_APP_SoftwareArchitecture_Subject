package com.example.zaloclone.api.model

data class AddFriendRequest(
    val current_email: String,
    val target_email: String
)

data class AddFriendResponse(
    val id: Int,
    val email: String,
    val username: String,
    val status: String
)

data class GetFriendListRequest(
    val email: String
)

data class FriendListResponse(
    val data: List<FriendRequestResponse>,
    val error_code: String? = null
)

data class FriendRequestResponse(
    val id: Int,
    val email: String,
    val username: String,
    val status: String
)