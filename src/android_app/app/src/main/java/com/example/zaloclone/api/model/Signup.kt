package com.example.zaloclone.api.model

data class RegisterRequest (
    val email: String,
    val username: String,
    val password: String
)

data class RegisterConfirmRequest (
    val email: String,
    val username: String,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponse (
    val email: String
)