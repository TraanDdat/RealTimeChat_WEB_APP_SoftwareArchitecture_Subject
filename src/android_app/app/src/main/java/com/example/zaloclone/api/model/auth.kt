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
    val profilePic: String
)