package com.example.zaloclone.api.repository

import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.api.model.LoginRequest
import com.example.zaloclone.api.model.RegisterRequest
import com.example.zaloclone.api.model.LoginResponse
import com.example.zaloclone.api.model.UpdateProfileRequest

class AuthRepository {
    suspend fun AuthCheck() = RetrofitClient.AuthApi.AuthCheck()

    suspend fun login(request: LoginRequest): LoginResponse = RetrofitClient.AuthApi.login(request)
    
    suspend fun signup(request: RegisterRequest) = RetrofitClient.AuthApi.signup(request)

    suspend fun logout() = RetrofitClient.AuthApi.logout()

    suspend fun updateProfile(request: UpdateProfileRequest) = RetrofitClient.AuthApi.updateProfile(request)
}