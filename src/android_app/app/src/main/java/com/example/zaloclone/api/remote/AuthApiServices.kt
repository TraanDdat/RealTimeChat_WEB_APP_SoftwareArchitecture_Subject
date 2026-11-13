
package com.example.zaloclone.api.remote

import com.example.zaloclone.api.model.AuthUser
import com.example.zaloclone.api.model.LoginRequest
import com.example.zaloclone.api.model.RegisterRequest
import com.example.zaloclone.api.model.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Headers
import com.example.zaloclone.ApiConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.zaloclone.api.model.LoginResponse

suspend fun loginUser(request: LoginRequest): Result<LoginResponse>
{
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.AuthApi.login(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

interface AuthApiServices {
    @POST(ApiConfig.API_AUTH_SIGNUP)
    suspend fun signup( @Body data: RegisterRequest ): AuthUser

    @Headers("X-Client-Type: android")
    @POST(ApiConfig.API_ATH_LOGIN)
    suspend fun login( @Body data: LoginRequest ): LoginResponse

    @POST(ApiConfig.API_AUTH_LOGOUT)
    suspend fun logout()

    @PUT(ApiConfig.API_AUTH_UPDATE_PROFILE)
    suspend fun updateProfile( @Body data: UpdateProfileRequest ): AuthUser

    @GET(ApiConfig.API_AUTH_CHECK)
    suspend fun AuthCheck(): AuthUser
}