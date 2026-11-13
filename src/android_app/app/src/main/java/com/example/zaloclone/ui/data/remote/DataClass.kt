package com.example.zaloclone.ui.data.remote

interface Userbase {
    val email: String
    val password: String
    val role: String
}

data class LoginRequest (
    override val email: String,
    override val password: String,
    override val role: String
) : Userbase

data class LoginResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val email: String
)

data class RegisterRequest (
    override val email: String,
    val username: String,
    override val password: String,
    override val role: String
) : Userbase

data class RegisterConfirmRequest (
    override val email: String,
    val username: String,
    override val password: String,
    override val role: String,
    val confirmPassword: String
) : Userbase

data class RegisterResponse (
    val email: String
)

data class ResetpwRequest (
    val email: String
)

data class ResetpwResponse (
    val email: String
)