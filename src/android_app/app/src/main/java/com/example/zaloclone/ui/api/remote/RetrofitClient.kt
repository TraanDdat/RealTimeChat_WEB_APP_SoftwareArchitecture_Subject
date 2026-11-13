package com.example.zaloclone.ui.api.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.zaloclone.ApiConfig

object RetrofitClient {
    private const val BASE_URL = ApiConfig.BASE_URL

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val AuthApi: AuthApiServices = retrofit.create(AuthApiServices::class.java)
    val RegisterApi: RegisterApiServices = retrofit.create(RegisterApiServices::class.java)
    val ResetPwApi: ResetPasswordApiServices = retrofit.create(ResetPasswordApiServices::class.java)
}