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
    val id: String,
    val senderId: Int,
    val senderName: String,
    val text: String,
    val timestamp: String
)

data class ChatState(
    val allContacts: List<ChatUser> = emptyList(),
    val chats: List<ChatUser> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val unreadNotifications: List<MessageNotification> = emptyList(),
    val userPresences: Map<Int, UserPresence> = emptyMap(),
    val activeTab: ActiveTab = ActiveTab.CHATS,
    val selectedUser: ChatUser? = null,
    val selectedGroup: Int? = null,
    val isUsersLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isSoundEnabled: Boolean = false,
    val errorMessage: String? = null
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
    private var groupMessageListener: Emitter.Listener? = null
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

                val isCurrentChat = selectedUser != null && newMessage.senderId == selectedUser.id

                if (isCurrentChat) {
                    if (!currentState.messages.any { it.id == newMessage.id }) {
                        _state.value = currentState.copy(
                            messages = currentState.messages + newMessage
                        )
                    }
                } else {
                    val senderId = newMessage.senderId
                    val existingNotifs = currentState.unreadNotifications
                    if (!existingNotifs.any { it.id == newMessage.id }) {
                        val senderContact = currentState.allContacts.find { it.id == senderId }
                            ?: currentState.chats.find { it.id == senderId }
                        val senderName = senderContact?.username ?: "Người dùng #$senderId"

                        val notif = MessageNotification(
                            id = newMessage.id,
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

    fun clearMessageNotification(notificationId: String) {
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
            selectedGroup = null,
            messages = emptyList(),
            isMessagesLoading = user != null
        )
        if (user != null) {
            getMessagesByUserId(user.id)
            subscribeToMessages()
        }
    }

    fun setSelectedGroup(groupId: Int?) {
        unsubscribeFromMessages()

        _state.value = _state.value.copy(
            selectedGroup = groupId,
            selectedUser = null,
            messages = emptyList(),
            isMessagesLoading = groupId != null
        )
        if (groupId != null) {
            getGroupMessages(groupId)
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

    fun getMessagesByUserId(userId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isMessagesLoading = true,
                errorMessage = null
            )
            try {
                val response = repository.getMessages(userId)
                if (_state.value.selectedUser?.id != userId) {return@launch}
                val mergedMessages = (response + _state.value.messages)
                    .distinctBy { it.id }
                    .sortedBy { it.createdAt }
                _state.value = _state.value.copy(messages = mergedMessages)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage ?: "Unable to get messages")
            } finally {
                if (_state.value.selectedUser?.id == userId) {
                    _state.value = _state.value.copy(isMessagesLoading = false)
                }
            }
        }
    }

    fun sendMessage(
        messageData: SendMessageRequest,
        currentUserId: Int
    ) {
        viewModelScope.launch {
            val currentState  = _state.value
            try {
                when {
                    currentState.selectedGroup != null -> {
                        val groupId = currentState.selectedGroup
                        val res_groupmsg = repository.sendGroupMessage(groupId, messageData)
                        if (_state.value.selectedGroup != groupId) {return@launch}
                        val new_groupmsg = res_groupmsg.toChatMessage()
                        _state.value = _state.value.copy(
                            messages = _state.value.messages.filter {it.id != new_groupmsg.id} + new_groupmsg
                            )
                    }
                    currentState.selectedUser != null -> {
                        val userId = currentState.selectedUser.id
                        val res_msg = repository.sendMessage(userId, messageData)
                        if (_state.value.selectedUser?.id != userId) {return@launch}
                        _state.value = _state.value.copy(
                            messages = _state.value.messages.filter {it.id != res_msg.id} + res_msg
                            )
                    }
                    else -> return@launch
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage ?: "Unable to send messages")
            }
        }
    }

    fun subscribeToMessages() {
        val socket = socketClient.getSocket() ?: return
        val currentState = _state.value
        if (currentState.selectedUser == null && currentState.selectedGroup == null) return
        unsubscribeFromMessages()
        // handle message listener (1-1 chat)
        messageListener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val state = _state.value
                val selectedUser = state.selectedUser ?: return@Listener
                if (newMessage.senderId != selectedUser.id) {return@Listener}
                if (state.messages.any { it.id == newMessage.id }) {return@Listener}
                _state.value = _state.value.copy(messages = state.messages + newMessage)
            } catch (e: Exception) {
                // Ignore invalid socket payload
            }
        }
        // handle group listener (1-n chat)
        groupMessageListener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val groupMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val state = _state.value
                val selectedGroup = state.selectedGroup ?: return@Listener
                if (groupMessage.groupId != selectedGroup) {return@Listener}
                if (state.messages.any { it.id == groupMessage.id }) {return@Listener}
                _state.value = _state.value.copy(messages = state.messages + groupMessage)
            } catch (e: Exception) {
                // Ignore invalid socket payload
            }
        }
        socket.on("newMessage", messageListener)
        socket.on("newGroupMessage", groupMessageListener)
    }

    fun unsubscribeFromMessages() {
        val socket = socketClient.getSocket()
        messageListener?.let {
            socket?.off("newMessage", it)
        }
        groupMessageListener?.let {
            socket?.off("newGroupMessage", it)
        }
        messageListener = null
        groupMessageListener = null
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
    
    fun getGroupMessages(groupId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMessagesLoading = true)
            try {
                if (_state.value.selectedGroup != groupId) return@launch
                val getgrmsg_response = repository.getGroupMessages(groupId)
                if (_state.value.selectedGroup != groupId) return@launch
                _state.value = _state.value.copy(messages = getgrmsg_response.map{it.toChatMessage()})
            } catch (e:Exception) {
                _state.value = _state.value.copy(errorMessage = e.message ?: "Could not get message of group")
            } finally {
                if (_state.value.selectedGroup == groupId) {
                    _state.value = _state.value.copy(isMessagesLoading = false)
                }
            }
        }
    }

    private fun GroupMessageDataResponse.toChatMessage(): ChatMessage {

        return ChatMessage(
            id = id,
            senderId = senderId,
            groupId = groupId,
            senderUsername = senderUsername,
            text = text,
            image = image,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
