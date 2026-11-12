package com.example.zaloclone.ui.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zaloclone.ui.data.remote.ResetPasswordApiServices

suspend fun resetPw(request: ResetpwRequest): Result<ResetpwResponse>
{
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.ResetPwApi.resetPw(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}