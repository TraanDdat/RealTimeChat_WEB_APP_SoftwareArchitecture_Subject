package com.example.zaloclone.ui.data.remote

import com.example.zaloclone.ApiConfig
import retrofit2.http.POST
import retrofit2.http.Body

interface AuthApiServices {
    @POST(ApiConfig.API_ATH_LOGIN)
    suspend fun login(
        @Body request : LoginRequest
    ): LoginResponse
}