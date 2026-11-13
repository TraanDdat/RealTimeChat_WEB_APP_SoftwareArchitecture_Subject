package com.example.zaloclone

import android.app.Application
import com.example.zaloclone.api.datastore.TokenManager
import com.example.zaloclone.api.remote.RetrofitClient

class ZaloApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        RetrofitClient.tokenManager = TokenManager(applicationContext)
    }
}