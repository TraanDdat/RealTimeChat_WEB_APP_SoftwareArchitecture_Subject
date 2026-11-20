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
import kotlinx.coroutines.flow.update
import com.example.zaloclone.ui.model.PresenceStatus
import com.example.zaloclone.ui.model.UserPresence
import com.example.zaloclone.session.SessionManager
import android.util.Log
import retrofit2.HttpException

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
    val unreadCounts: Map<Int, Int> = emptyMap(),
    val groupUnreadCounts: Map<Int, Int> = emptyMap(),
    val activeTab: ActiveTab = ActiveTab.CHATS,
    val selectedUser: ChatUser? = null,
    val selectedGroup: Int? = null,
    val isUsersLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isSoundEnabled: Boolean = false,
    val errorMessage: String? = null,
    val emotion: String? = null
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
    private var globalGroupMessageListener: Emitter.Listener? = null

    fun startGlobalSocketListener() {
        val socket = socketClient.getSocket() ?: run {
            Log.e("CHAT_SOCKET", "Socket is null")
            return
        }

        globalMessageListener?.let {
            socket.off("newMessage", it)
        }

        globalGroupMessageListener?.let {
            socket.off("newGroupMessage", it)
        }

        val listener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val currentUserId = sessionManager.currentUserId
                if (newMessage.senderId == currentUserId) {
                    return@Listener
                }
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
                    val currentCount = currentState.unreadCounts[senderId] ?: 0
                    _state.value = currentState.copy(
                        unreadCounts = currentState.unreadCounts + (
                            senderId to currentCount + 1
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e("CHAT_SOCKET", "Error processing message", e)
            }
        }

        val groupListener = Emitter.Listener { args ->
            if (args.isEmpty()) return@Listener
            try {
                val newMessage = gson.fromJson(
                    args[0].toString(),
                    ChatMessage::class.java
                )
                val groupId = newMessage.groupId ?: return@Listener
                val currentState = _state.value
                if (currentState.selectedGroup == groupId) return@Listener
                val currentCount = currentState.groupUnreadCounts[groupId] ?: 0
                _state.update {
                    it.copy(
                        groupUnreadCounts = it.groupUnreadCounts + (
                            groupId to currentCount + 1
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("GROUP_CHAT_SOCKET", "Error processing group message", e)
            }
        }

        globalMessageListener = listener
        globalGroupMessageListener = groupListener
        socket.on("newMessage", listener)
        socket.on("newGroupMessage", groupListener)
    }

    fun clearNotificationsForUser(senderId: Int) {
        viewModelScope.launch {
            try {
                repository.markRead(senderId)
                val updatedCounts = _state.value.unreadCounts.toMutableMap()
                updatedCounts.remove(senderId)
                _state.value = _state.value.copy(
                    unreadCounts = updatedCounts,
                )
            } catch (e: Exception) {
                Log.e("MARK_READ", "Failed to mark as read", e)
            }
        }
    }

    fun clearNotificationsForGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                repository.markGroupRead(groupId)
                val updatedGroupCounts = _state.value.groupUnreadCounts.toMutableMap()
                updatedGroupCounts.remove(groupId)
                _state.value = _state.value.copy(
                    groupUnreadCounts = updatedGroupCounts
                )
            } catch (e: Exception) {
                Log.e("GROUP_MARK_READ", "Failed to mark group as read", e)
            }
        }
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
                val messages = response
                    .distinctBy { it.id }
                    .sortedBy { it.createdAt }
                _state.update { it.copy(messages = messages) }
                Log.d("CHAT_DEBUG", "State updated size=${_state.value.messages.size}")
            } catch (e: Exception) {
                if (_state.value.selectedUser?.id == userId) {
                    _state.update {
                        it.copy(
                            errorMessage = e.localizedMessage
                                ?: "Unable to get messages"
                        )
                    }
                }
            } finally {
                if (_state.value.selectedUser?.id == userId) {
                _state.update { it.copy(isMessagesLoading = false) }
                }
            }
        }
    }

    fun sendMessage(
        messageData: SendMessageDataRequest,
        currentUserId: Int
    ) {
        viewModelScope.launch {
            val currentState = _state.value
            try {
                when {
                    currentState.selectedGroup != null -> {
                        val groupId = currentState.selectedGroup
                        val res_groupmsg = repository.sendGroupMessage(groupId, messageData)
                        if (_state.value.selectedGroup != groupId) {return@launch}
                        val new_groupmsg = res_groupmsg.toChatMessage()
                        _state.update { state ->
                            if (state.messages.any { it.id == new_groupmsg.id }) {
                                state
                            } else {
                                state.copy(messages = (state.messages + new_groupmsg).sortedBy { it.createdAt })
                            }
                        }
                    }
                    currentState.selectedUser != null -> {
                        val userId = currentState.selectedUser.id
                        val res_msg = repository.sendMessage(userId,messageData)
                        if (_state.value.selectedUser?.id != userId) {return@launch}
                        _state.update { state ->
                            if (state.messages.any { it.id == res_msg.id }) {
                                state
                            } else {
                                state.copy(messages = (state.messages + res_msg).sortedBy { it.createdAt })
                            }
                        }
                    }
                    else -> return@launch
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                _state.update {it.copy(errorMessage = errorBody ?: "Unable to send messages")}
            } catch (e: Exception) {
                Log.e("CHAT_API", "Send message failed", e)

                _state.update {it.copy(errorMessage = e.localizedMessage?: "Unable to send messages")}}
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
                _state.update { state ->
                    val selectedUser = state.selectedUser?: return@update state
                    if (newMessage.senderId != selectedUser.id) {return@update state}
                    if (state.messages.any { it.id == newMessage.id }) {return@update state}
                    state.copy(messages = (state.messages + newMessage).sortedBy { it.createdAt })
                }
            } catch (e: Exception) {
                Log.e("CHAT_SOCKET", "Invalid message payload", e)
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
                _state.update { state ->
                    val selectedGroup = state.selectedGroup?: return@update state
                    if (groupMessage.groupId != selectedGroup) {return@update state}
                    if (state.messages.any { it.id == groupMessage.id }) {return@update state}
                    state.copy(messages = (state.messages + groupMessage).sortedBy { it.createdAt })
                }
            } catch (e: Exception) {
                Log.e("GROUP_CHAT_SOCKET", "Invalid group message payload", e)
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
    
    override fun onCleared() {
        unsubscribeFromMessages()
        val socket = socketClient.getSocket()
        globalMessageListener?.let {
            socket?.off("newMessage", it)
        }
        globalGroupMessageListener?.let {
            socket?.off("newGroupMessage", it)
        }
        globalMessageListener = null
        globalGroupMessageListener = null
        super.onCleared()
    }

    fun getCurrentUserId(): Int? = sessionManager.currentUserId

    fun getGroupMessages(groupId: Int) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isMessagesLoading = true,
                    errorMessage = null
                )
            }
            try {
                if (_state.value.selectedGroup != groupId) {return@launch}
                val response = repository.getGroupMessages(groupId)
                if (_state.value.selectedGroup != groupId) {return@launch}
                val messages = response
                    .map { it.toChatMessage() }
                    .distinctBy { it.id }
                    .sortedBy { it.createdAt }
                _state.update {it.copy(messages = messages)}
            } catch (e: Exception) {
                if (_state.value.selectedGroup == groupId) {
                    _state.update {it.copy(errorMessage = e.localizedMessage?: "Could not get messages of group")}
                }
            } finally {
                if (_state.value.selectedGroup == groupId) {_state.update {it.copy(isMessagesLoading = false)}
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

    fun clearSelectedChat() {
        unsubscribeFromMessages()
        _state.value = _state.value.copy(
            selectedUser = null,
            selectedGroup = null,
            messages = emptyList(),
            isMessagesLoading = false
        )
    }

    fun unreadCount() {
        viewModelScope.launch {
            try {
                val response = repository.getUnreadCounts()
                Log.d("UNREAD_DEBUG", "Users = ${response.users}")
                Log.d("UNREAD_DEBUG", "Groups = ${response.groups}")
                val userCounts = response.users.mapNotNull { (id, count) ->
                    id.toIntOrNull()?.let { it to count }
                }.toMap()
                val groupCounts = response.groups.mapNotNull { (id, count) ->
                    id.toIntOrNull()?.let { it to count }
                }.toMap()
                _state.update {
                    it.copy(
                        unreadCounts = userCounts,
                        groupUnreadCounts = groupCounts
                    )
                }
            } catch (e: Exception) {
                Log.e("UNREAD_COUNT", "Failed to sync unread counts", e)
            }
        }
    }

    fun clearUnreadCount(senderId: Int) {
        viewModelScope.launch {
            try {
                repository.markRead(senderId)
                val updatedCounts = _state.value.unreadCounts.toMutableMap()
                updatedCounts.remove(senderId)
                _state.value = _state.value.copy(
                    unreadCounts = updatedCounts,
                )
            } catch (e: Exception) {
                Log.e("MARK_READ", "Failed to mark messages as read", e)
            }
        }
    }

    fun clearGroupUnreadCount(groupId: Int) {
        viewModelScope.launch {
            try {
                repository.markGroupRead(groupId)
                val updatedCounts = _state.value.groupUnreadCounts.toMutableMap()
                updatedCounts.remove(groupId)
                _state.value = _state.value.copy(
                    groupUnreadCounts = updatedCounts
                )
            } catch (e: Exception) {
                Log.e("GROUP_MARK_READ", "Failed to mark group messages as read", e)
            }
        }
    }
}
