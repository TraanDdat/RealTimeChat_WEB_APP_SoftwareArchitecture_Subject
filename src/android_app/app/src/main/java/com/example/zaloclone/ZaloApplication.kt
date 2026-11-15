package com.example.zaloclone

import android.app.Application
import com.example.zaloclone.api.remote.RetrofitClient
import com.example.zaloclone.di.AppContainer

class ZaloApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        RetrofitClient.tokenManager = container.tokenManager
    }
}