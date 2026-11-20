package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.*

interface IAuthRepository {
    suspend fun AuthCheck(): AuthUser
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun signup(request: RegisterRequest): AuthUser
    suspend fun logout()
    suspend fun updateProfile(request: UpdateProfileRequest): UpdateProfileResponse
}