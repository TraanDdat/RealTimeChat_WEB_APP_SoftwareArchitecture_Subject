package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zaloclone.api.repository.ChatRepository
import com.example.zaloclone.socket.SocketClient

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val socketClient: SocketClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(
                repository,
                socketClient
            ) as T
        }
        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}