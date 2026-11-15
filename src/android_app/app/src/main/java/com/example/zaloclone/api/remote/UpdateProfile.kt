package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.UpdateProfileRequest
import com.example.zaloclone.api.model.AuthUser

suspend fun updateProfileUser(
    data: UpdateProfileRequest
): Result<AuthUser> {
    return try {
        Result.success(RetrofitClient.AuthApi.updateProfile(data))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
