package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zaloclone.api.repository.IAuthRepository
import com.example.zaloclone.api.datastore.ITokenManager
import com.example.zaloclone.socket.ISocketClient
import com.example.zaloclone.socket.ISocketService

class AuthViewModelFactory(
    private val repository: IAuthRepository,
    private val tokenManager: ITokenManager,
    private val socketClient: ISocketClient,
    private val socketService: ISocketService
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