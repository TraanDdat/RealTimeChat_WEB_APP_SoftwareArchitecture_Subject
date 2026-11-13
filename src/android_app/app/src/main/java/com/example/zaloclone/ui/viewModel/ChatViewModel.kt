package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.ChatRepository
import io.socket.emitter.Emitter
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.zaloclone.socket.SocketClient

enum class ActiveTab {
    CHATS,
    CONTACTS
}

data class ChatState(
    val allContacts: List<ChatUser> = emptyList(),
    val chats: List<ChatUser> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val activeTab: ActiveTab = ActiveTab.CHATS,
    val selectedUser: ChatUser? = null,
    val isUsersLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isSoundEnabled: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val socketClient: SocketClient
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val gson = Gson()
    private var messageListener: Emitter.Listener? = null

    fun toggleSound() {
        _state.value = _state.value.copy(
            isSoundEnabled = !_state.value.isSoundEnabled
        )
    }

    fun setActiveTab(tab: ActiveTab) {
        _state.value = _state.value.copy(
            activeTab = tab
        )
    }

    fun setSelectedUser(user: ChatUser?) {
        unsubscribeFromMessages()

        _state.value = _state.value.copy(
            selectedUser = user,
            messages = emptyList(),
            isMessagesLoading = user != null
        )
        if (user != null) {
            getMessagesByUserId(user.id.toInt())
            subscribeToMessages()
        }
    }

    fun getAllContacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUsersLoading = true,
                errorMessage = null
            )
            try {
                val contacts = repository.getContacts()

                _state.value = _state.value.copy(
                    allContacts = contacts
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage
                        ?: "Unable to get contacts"
                )
            } finally {
                _state.value = _state.value.copy(
                    isUsersLoading = false
                )
            }
        }
    }

    fun getMyChatPartners() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUsersLoading = true,
                errorMessage = null
            )
            try {
                val chats = repository.getChatPartners()

                _state.value = _state.value.copy(
                    chats = chats
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage
                        ?: "Unable to get chat partners"
                )
            } finally {
                _state.value = _state.value.copy(
                    isUsersLoading = false
                )
            }
        }
    }

    fun getMessagesByUserId(userId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isMessagesLoading = true,
                errorMessage = null
            )
            try {
                val messages = repository.getMessages(userId)

                if (_state.value.selectedUser?.id?.toInt() != userId) {
                    return@launch
                }
                _state.value = _state.value.copy(
                    messages = messages
                )
            } catch (e: Exception) {
                if (_state.value.selectedUser?.id?.toInt() == userId) {
                    _state.value = _state.value.copy(
                        errorMessage = e.localizedMessage
                            ?: "Unable to get messages"
                    )
                }
            } finally {
                if (_state.value.selectedUser?.id?.toInt() == userId) {
                    _state.value = _state.value.copy(
                        isMessagesLoading = false
                    )
                }
            }
        }
    }

    fun sendMessage(
        messageData: SendMessageRequest,
        currentUserId: Int
    ) {
        val selectedUser = _state.value.selectedUser ?: return
        val receiverId = selectedUser.id.toInt()
        viewModelScope.launch {
            val tempId = -System.currentTimeMillis()
            val optimisticMessage = ChatMessage(
                id = tempId.toInt(),
                senderId = currentUserId,
                receiverId = receiverId,
                text = messageData.text,
                image = messageData.image,
                createdAt = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    java.util.Locale.US
                ).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()),
                updateAt = null,
                isOptimistic = true
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + optimisticMessage
            )
            try {
                val response = repository.sendMessage(
                    receiverId,
                    messageData
                )
                if (_state.value.selectedUser?.id?.toInt() != receiverId) {
                    return@launch
                }
                val updatedMessages = _state.value.messages
                    .filter {
                        it.id != tempId.toInt() &&
                        it.id != response.id
                    } + response
                _state.value = _state.value.copy(
                    messages = updatedMessages
                )
            } catch (e: Exception) {
                if (_state.value.selectedUser?.id?.toInt() == receiverId) {
                    _state.value = _state.value.copy(
                        messages = _state.value.messages.filter {
                            it.id != tempId.toInt()
                        },
                        errorMessage = e.localizedMessage
                            ?: "Unable to send message"
                    )
                }
            }
        }
    }

    fun subscribeToMessages() {
        val socket = socketClient.getSocket() ?: return
        if (_state.value.selectedUser == null) return
        unsubscribeFromMessages()
        val listener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val selectedUser = _state.value.selectedUser
                    ?: return@Listener
                if (newMessage.senderId != selectedUser.id.toInt()) {
                    return@Listener
                }
                if (_state.value.messages.any {
                        it.id == newMessage.id
                    }) {
                    return@Listener
                }
                _state.value = _state.value.copy(
                    messages = _state.value.messages + newMessage
                )
            } catch (e: Exception) {
                // Ignore invalid socket payload
            }
        }
        messageListener = listener
        socket.on("newMessage", listener)
    }

    fun unsubscribeFromMessages() {
        val socket = socketClient.getSocket()
        messageListener?.let {
            socket?.off("newMessage", it)
        }
        messageListener = null
    }

    fun clearError() {
        _state.value = _state.value.copy(
            errorMessage = null
        )
    }

    override fun onCleared() {
        unsubscribeFromMessages()
        super.onCleared()
    }
}
