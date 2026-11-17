package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.RegisterRequest
import com.example.zaloclone.api.model.RegisterConfirmRequest
import com.example.zaloclone.api.model.AuthUser

suspend fun signup(request: RegisterRequest): Result<AuthUser> {
    return try {
        val response = RetrofitClient.AuthApi.signup(request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun handleRegister(
    request: RegisterConfirmRequest
): Result<AuthUser> {

    if (
        request.email.isBlank() ||
        request.password.isBlank() ||
        request.confirmPassword.isBlank()
    ) {
        return Result.failure(
            Exception("Please fill email and password")
        )
    }

    if (request.password != request.confirmPassword) {
        return Result.failure(
            Exception("Password do not match")
        )
    }

    return signup(
        RegisterRequest(
            email = request.email,
            username = request.username,
            password = request.password
        )
    )
}