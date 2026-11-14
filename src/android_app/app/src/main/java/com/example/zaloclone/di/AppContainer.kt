package com.example.zaloclone.di

import android.content.Context
import com.example.zaloclone.api.datastore.ITokenManager
import com.example.zaloclone.api.datastore.TokenManager
import com.example.zaloclone.api.repository.AuthRepository
import com.example.zaloclone.api.repository.ChatRepository
import com.example.zaloclone.api.repository.IAuthRepository
import com.example.zaloclone.api.repository.IChatRepository
import com.example.zaloclone.socket.ISocketClient
import com.example.zaloclone.socket.ISocketService
import com.example.zaloclone.socket.SocketService
import com.example.zaloclone.socket.socketClient

class AppContainer(private val applicationContext: Context) {

    val tokenManager: ITokenManager by lazy {
        TokenManager(applicationContext)
    }

    val authRepository: IAuthRepository by lazy {
        AuthRepository()
    }

    val chatRepository: IChatRepository by lazy {
        ChatRepository()
    }

    val socketClientInstance: ISocketClient by lazy {
        socketClient
    }

    val socketServiceInstance: ISocketService by lazy {
        SocketService(socketClientInstance)
    }
}