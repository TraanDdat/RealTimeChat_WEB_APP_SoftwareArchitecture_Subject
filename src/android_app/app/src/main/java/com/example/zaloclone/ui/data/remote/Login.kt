package com.example.zaloclone.ui.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zaloclone.ui.data.remote.AuthApiServices

fun login(
    request: LoginRequest,
    onResult: (LoginResponse) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.AuthApi.login(request)
            withContext(Dispatchers.Main) {
                println("Android login success: ${response.email}")
                onResult(response)
            }
        } catch (e: Exception) {
            println("Android login failed: ${e.message}")
        }
    }
}