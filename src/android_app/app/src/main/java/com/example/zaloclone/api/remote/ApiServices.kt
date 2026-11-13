package com.example.zaloclone.api.remote

import com.example.zaloclone.ApiConfig
import retrofit2.http.POST
import retrofit2.http.Body
import com.example.zaloclone.api.model.ResetpwRequest
import com.example.zaloclone.api.model.ResetpwResponse
import com.example.zaloclone.api.model.AuthUser
import retrofit2.http.GET

interface ResetPasswordApiServices {
    @POST(ApiConfig.API_AUTH_RESETPW)
    suspend fun resetPw( @Body request : ResetpwRequest ): ResetpwResponse
}