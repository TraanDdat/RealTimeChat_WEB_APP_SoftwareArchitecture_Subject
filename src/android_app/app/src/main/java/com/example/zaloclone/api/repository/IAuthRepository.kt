package com.example.zaloclone.api.repository

import com.example.zaloclone.api.model.AuthUser
import com.example.zaloclone.api.model.LoginRequest
import com.example.zaloclone.api.model.LoginResponse
import com.example.zaloclone.api.model.RegisterRequest
import com.example.zaloclone.api.model.UpdateProfileRequest

interface IAuthRepository {
    suspend fun AuthCheck(): AuthUser
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun signup(request: RegisterRequest): AuthUser
    suspend fun logout()
    suspend fun updateProfile(request: UpdateProfileRequest): AuthUser
}