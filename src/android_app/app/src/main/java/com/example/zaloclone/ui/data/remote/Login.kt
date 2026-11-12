package com.example.zaloclone.ui.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zaloclone.ui.data.remote.AuthApiServices

suspend fun loginUser(request: LoginRequest): Result<LoginResponse>
{
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.AuthApi.loginUser(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}