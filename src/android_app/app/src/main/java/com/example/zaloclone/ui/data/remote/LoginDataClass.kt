package com.example.zaloclone.ui.data.remote

data class LoginRequest (
    val email: String,
    val password: String,
    val role: String
)

data class LoginResponse (
    val email: String,
    val password: String
)