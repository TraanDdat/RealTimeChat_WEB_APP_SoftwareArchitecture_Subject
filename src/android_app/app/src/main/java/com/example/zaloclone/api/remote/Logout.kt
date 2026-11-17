package com.example.zaloclone.api.remote

suspend fun logoutUser(): Result<Unit> {
    return try {
        RetrofitClient.AuthApi.logout()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
