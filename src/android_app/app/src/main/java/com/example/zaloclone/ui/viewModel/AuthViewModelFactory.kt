package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zaloclone.api.repository.AuthRepository
import com.example.zaloclone.api.datastore.TokenManager
import com.example.zaloclone.socket.SocketClient
import com.example.zaloclone.socket.SocketService

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager,
    private val socketClient: SocketClient,
    private val socketService: SocketService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                repository = repository,
                tokenManager = tokenManager,
                socketClient = socketClient,
                socketService = socketService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}