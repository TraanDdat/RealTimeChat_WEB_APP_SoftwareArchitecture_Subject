package com.example.zaloclone.ui.api.remote

import com.example.zaloclone.ApiConfig
import retrofit2.http.POST
import retrofit2.http.Body

interface AuthApiServices {
    @POST(ApiConfig.API_ATH_LOGIN)
    suspend fun loginUser(
        @Body request : LoginRequest
    ): LoginResponse
}

interface RegisterApiServices {
    @POST(ApiConfig.API_AUTH_REGISTER)
    suspend fun register(
        @Body request : RegisterRequest
    ): RegisterResponse
}

interface ResetPasswordApiServices {
    @POST(ApiConfig.API_AUTH_RESETPW)
    suspend fun resetPw(
        @Body request : ResetpwRequest
    ): ResetpwResponse
}