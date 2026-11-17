package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IChatRepository
import com.example.zaloclone.socket.ISocketClient
import io.socket.emitter.Emitter
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import com.example.zaloclone.ui.model.PresenceStatus
import com.example.zaloclone.ui.model.UserPresence
import com.example.zaloclone.session.SessionManager

enum class ActiveTab {
    CHATS,
    CONTACTS
}

data class MessageNotification(
    val id: Int,
    val senderId: Int,
    val senderName: String,
    val text: String,
    val timestamp: String
)

data class ChatState(
    val allContacts: List<ChatUser> = emptyList(),
    val chats: List<ChatUser> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val unreadNotifications: List<MessageNotification> = emptyList(),
    val userPresences: Map<Int, UserPresence> = emptyMap(),
    val activeTab: ActiveTab = ActiveTab.CHATS,
    val selectedUser: ChatUser? = null,
    val selectedConversation: Conversation? = null,
    val isUsersLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isSoundEnabled: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: Int? = null
)

class ChatViewModel(
    private val repository: IChatRepository,
    private val socketClient: ISocketClient,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val gson = Gson()
    private var messageListener: Emitter.Listener? = null
    private var globalMessageListener: Emitter.Listener? = null

    init {
        startGlobalSocketListener()
    }

    fun updateUserPresences(presences: Map<Int, UserPresence>) {
        _state.value = _state.value.copy(userPresences = presences)
    }

    fun startGlobalSocketListener() {
        val socket = socketClient.getSocket() ?: return
        globalMessageListener?.let { socket.off("newMessage", it) }

        val listener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val currentState = _state.value
                val selectedUser = currentState.selectedUser
                val selectedConversation = currentState.selectedConversation

                val isCurrentChat = if (selectedConversation != null) {
                    newMessage.conversationId == selectedConversation.id
                } else if (selectedUser != null) {
                    newMessage.senderId == selectedUser.id
                } else {
                    false
                }

                if (isCurrentChat) {
                    if (!currentState.messages.any { it.id == newMessage.id }) {
                        _state.value = currentState.copy(
                            messages = currentState.messages + newMessage
                        )
                    }
                } else {
                    val senderId = newMessage.senderId
                    val existingNotifs = currentState.unreadNotifications
                    if (!existingNotifs.any { it.id == newMessage.id.toInt() }) {
                        val senderContact = currentState.allContacts.find { it.id == senderId }
                            ?: currentState.chats.find { it.id == senderId }
                        val senderName = senderContact?.username ?: "Người dùng #$senderId"

                        val notif = MessageNotification(
                            id = newMessage.id.toInt(),
                            senderId = senderId,
                            senderName = senderName,
                            text = newMessage.text ?: "[Hình ảnh]",
                            timestamp = newMessage.createdAt
                        )
                        _state.value = currentState.copy(
                            unreadNotifications = existingNotifs + notif
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        globalMessageListener = listener
        socket.on("newMessage", listener)
    }

    fun clearMessageNotification(notificationId: Int) {
        val updatedNotifications = _state.value.unreadNotifications.filter { it.id != notificationId }
        _state.value = _state.value.copy(unreadNotifications = updatedNotifications)
    }

    fun clearNotificationsForUser(senderId: Int) {
        val updatedNotifications = _state.value.unreadNotifications.filter { it.senderId != senderId }
        _state.value = _state.value.copy(unreadNotifications = updatedNotifications)
    }

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
            selectedConversation = null,
            messages = emptyList(),
            isMessagesLoading = user != null
        )
        if (user != null) {
            getMessagesByUserId(user.id)
            subscribeToMessages()
        }
    }

    fun setSelectedConversation(conversation: Conversation?) {
        unsubscribeFromMessages()

        _state.value = _state.value.copy(
            selectedConversation = conversation,
            selectedUser = null,
            messages = emptyList(),
            isMessagesLoading = conversation != null
        )
        if (conversation != null) {
            getMessagesByConversation(conversation.id)
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
                    errorMessage = e.localizedMessage ?: "Unable to get contacts"
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
                    errorMessage = e.localizedMessage ?: "Unable to get chat partners"
                )
            } finally {
                _state.value = _state.value.copy(
                    isUsersLoading = false
                )
            }
        }
    }

    fun getConversations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUsersLoading = true,
                errorMessage = null
            )
            try {
                val conversations = repository.getConversations()
                _state.value = _state.value.copy(
                    conversations = conversations
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to get conversations"
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
                        errorMessage = e.localizedMessage ?: "Unable to get messages"
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

    fun getMessagesByConversation(conversationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isMessagesLoading = true,
                errorMessage = null
            )
            try {
                val messages = repository.getMessagesByConversation(conversationId)
                if (_state.value.selectedConversation?.id != conversationId) {
                    return@launch
                }
                _state.value = _state.value.copy(
                    messages = messages
                )
            } catch (e: Exception) {
                if (_state.value.selectedConversation?.id == conversationId) {
                    _state.value = _state.value.copy(
                        errorMessage = e.localizedMessage ?: "Unable to get messages"
                    )
                }
            } finally {
                if (_state.value.selectedConversation?.id == conversationId) {
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
        val selectedUser = _state.value.selectedUser
        val selectedConversation = _state.value.selectedConversation

        if (selectedConversation != null) {
            val convId = selectedConversation.id
            viewModelScope.launch {
                val tempId = "temp_${System.currentTimeMillis()}"
                val optimisticMessage = ChatMessage(
                    id = tempId,
                    senderId = currentUserId,
                    receiverId = null,
                    conversationId = convId,
                    text = messageData.text,
                    image = messageData.image,
                    createdAt = SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.US
                    ).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
                    updatedAt = null,
                    isOptimistic = true
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + optimisticMessage
                )
                try {
                    val response = repository.sendMessageToConversation(convId, messageData)
                    if (_state.value.selectedConversation?.id != convId) {
                        return@launch
                    }
                    val updatedMessages = _state.value.messages.filter {
                        it.id != tempId && it.id != response.id
                    } + response
                    _state.value = _state.value.copy(
                        messages = updatedMessages
                    )
                } catch (e: Exception) {
                    if (_state.value.selectedConversation?.id == convId) {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages.filter { it.id != tempId },
                            errorMessage = e.localizedMessage ?: "Unable to send message"
                        )
                    }
                }
            }
        } else if (selectedUser != null) {
            val receiverId = selectedUser.id
            viewModelScope.launch {
                val tempId = "temp_${System.currentTimeMillis()}"
                val optimisticMessage = ChatMessage(
                    id = tempId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    conversationId = null,
                    text = messageData.text,
                    image = messageData.image,
                    createdAt = SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.US
                    ).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
                    updatedAt = null,
                    isOptimistic = true
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + optimisticMessage
                )
                try {
                    val response = repository.sendMessage(receiverId, messageData)
                    if (_state.value.selectedUser?.id?.toInt() != receiverId) {
                        return@launch
                    }
                    val updatedMessages = _state.value.messages.filter {
                        it.id != tempId && it.id != response.id
                    } + response
                    _state.value = _state.value.copy(
                        messages = updatedMessages
                    )
                } catch (e: Exception) {
                    if (_state.value.selectedUser?.id?.toInt() == receiverId) {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages.filter { it.id != tempId },
                            errorMessage = e.localizedMessage ?: "Unable to send message"
                        )
                    }
                }
            }
        }
    }

    fun subscribeToMessages() {
        val socket = socketClient.getSocket() ?: return
        if (_state.value.selectedUser == null && _state.value.selectedConversation == null) return
        unsubscribeFromMessages()
        val listener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val selectedUser = _state.value.selectedUser
                val selectedConversation = _state.value.selectedConversation

                if (selectedConversation != null) {
                    if (newMessage.conversationId != selectedConversation.id) {
                        return@Listener
                    }
                } else if (selectedUser != null) {
                    if (newMessage.senderId != selectedUser.id.toInt()) {
                        return@Listener
                    }
                } else {
                    return@Listener
                }

                if (_state.value.messages.any { it.id == newMessage.id }) {
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

    fun deleteFriend(friendId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(errorMessage = null)
            try {
                repository.deleteFriend(friendId)
                getMyChatPartners()
                getAllContacts()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to delete friend"
                )
            }
        }
    }

    override fun onCleared() {
        unsubscribeFromMessages()
        super.onCleared()
    }

    fun getCurrentUserId(): Int? = sessionManager.currentUserId
}
