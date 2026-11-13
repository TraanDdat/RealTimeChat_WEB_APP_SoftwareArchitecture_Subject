package com.example.zaloclone.api.model

data class LoginRequest (
    val email: String,
    val password: String
)

data class LoginResponse(
    val data: LoginData,
    val error_code: String?
)

data class LoginData(
    val id: Int,
    val email: String,
    val username: String,
    val role: String,
    val profilePic: String?,
    val access_token: String?
)