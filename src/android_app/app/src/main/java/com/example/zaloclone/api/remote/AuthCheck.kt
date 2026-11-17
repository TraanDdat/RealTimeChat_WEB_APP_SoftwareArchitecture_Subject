package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.AuthUser

suspend fun AuthCheck(): Result<AuthUser> {
    return try {
        val response: AuthUser = RetrofitClient.AuthApi.AuthCheck()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}