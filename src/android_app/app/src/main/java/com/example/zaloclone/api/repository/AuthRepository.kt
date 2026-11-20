package com.example.zaloclone.api.repository

import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.api.remote.AuthApiServices
import com.example.zaloclone.api.model.*

class AuthRepository(
    private val authApi: AuthApiServices = RetrofitClient.AuthApi
) : IAuthRepository {
    override suspend fun AuthCheck(): AuthUser = authApi.AuthCheck()

    override suspend fun login(request: LoginRequest): LoginResponse = authApi.login(request)

    override suspend fun signup(request: RegisterRequest): AuthUser = authApi.signup(request)

    override suspend fun logout() = authApi.logout()

    override suspend fun updateProfile(request: UpdateProfileRequest): UpdateProfileResponse = authApi.updateProfile(request)
}