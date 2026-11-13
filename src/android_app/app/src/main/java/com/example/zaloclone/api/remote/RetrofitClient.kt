package com.example.zaloclone.api.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.zaloclone.ApiConfig
import com.example.zaloclone.api.datastore.TokenManager
import okhttp3.OkHttpClient
import kotlinx.coroutines.runBlocking
import android.util.Log

object RetrofitClient {
    private const val BASE_URL = ApiConfig.BASE_URL
    lateinit var tokenManager: TokenManager
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val token = runCatching {
                    runBlocking {
                        tokenManager.getToken()
                    }
                }.getOrNull()
                Log.d("AUTH", "Token exists: ${!token.isNullOrBlank()}")
                val request = originalRequest.newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // QUAN TRỌNG
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val AuthApi: AuthApiServices by lazy {
        retrofit.create(AuthApiServices::class.java)
    }

    val ResetPwApi: ResetPasswordApiServices by lazy {
        retrofit.create(ResetPasswordApiServices::class.java)
    }

    val ChatApi: ChatApiServices by lazy {
        retrofit.create(ChatApiServices::class.java)
    }
}