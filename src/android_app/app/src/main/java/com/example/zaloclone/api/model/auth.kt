package com.example.zaloclone.api.model

import com.google.gson.annotations.SerializedName

data class AuthUser(
    @SerializedName("_id") val id: Int,
    val username: String,
    val email: String,
    val profilePic: String? = null,
    val access_token: String
)

data class UpdateProfileRequest(
    val profilePic: String? = null,
    val username: String? = null,
    val id: Int
)

data class UpdateProfileResponse(
    val profilePic: String? = null,
    val username: String? = null,
    val id: Int? = null
)

data class UserProfile(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val avatarBgColorHex: Long = 0xFF38C7FF,
    val avatarUri: String? = null
)