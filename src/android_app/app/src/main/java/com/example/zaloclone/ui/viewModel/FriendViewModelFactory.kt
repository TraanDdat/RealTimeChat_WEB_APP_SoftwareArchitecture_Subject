package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zaloclone.socket.ISocketClient
import com.example.zaloclone.socket.ISocketService
import com.example.zaloclone.session.SessionManager
import com.example.zaloclone.api.repository.IFriendRepository

class FriendViewModelFactory(
    private val repository: IFriendRepository,
    private val socketClient: ISocketClient,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(FriendViewModel::class.java)) {
            return FriendViewModel(
                repository,
                socketClient,
                sessionManager
            ) as T
        }
        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}