package com.example.zaloclone.ui.api.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zaloclone.ui.api.remote.RegisterApiServices

suspend fun register(request: RegisterRequest): Result<RegisterResponse>
{
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.RegisterApi.register(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

suspend fun handleRegister(request: RegisterConfirmRequest): Result<RegisterResponse>
{
    if (request.email.isBlank() || request.password.isBlank() || request.confirmPassword.isBlank())
    {
        return Result.failure(Exception("Please fill email and password"))
    }
    if (request.password != request.confirmPassword)
    {
        return Result.failure(Exception("Password do not match"))
    }
    return register(RegisterRequest(email = request.email, username = request.username, password = request.password, role = request.role))
}